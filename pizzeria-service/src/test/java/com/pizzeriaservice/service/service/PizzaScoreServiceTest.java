package com.pizzeriaservice.service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pizzeriaservice.api.dto.PizzaScoreCreateRequest;
import com.pizzeriaservice.api.dto.PizzaType;
import com.pizzeriaservice.service.converter.RestDomainConverter;
import com.pizzeriaservice.service.domain.model.PizzaScore;
import com.pizzeriaservice.service.repository.inmemory.InMemoryPizzaScoreRepository;
import com.pizzeriaservice.service.support.TimeProvider;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class PizzaScoreServiceTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID OTHER_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000002");

  private InMemoryPizzaScoreRepository repository;
  private TestTimeProvider timeProvider;
  private PizzaScoreService pizzaScoreService;

  @BeforeEach
  void setUp() {
    repository = new InMemoryPizzaScoreRepository();
    timeProvider = new TestTimeProvider(Instant.parse("2024-01-01T00:00:00Z"));
    pizzaScoreService = new PizzaScoreService(repository, timeProvider, new RestDomainConverter());
  }

  @Test
  void createAndRetrieveScores() {
    UUID userId = UUID.randomUUID();
    UUID pizzaId = UUID.randomUUID();

    StepVerifier.create(
            pizzaScoreService.create(
                userId,
                DEFAULT_PIZZERIA_ID,
                new PizzaScoreCreateRequest(pizzaId, PizzaType.TEMPLATE, 5, "Great!")))
        .assertNext(
            response -> {
              assertThat(response.userId()).isEqualTo(userId);
              assertThat(response.pizzaId()).isEqualTo(pizzaId);
              assertThat(response.score()).isEqualTo(5);
              assertThat(response.comment()).isEqualTo("Great!");
            })
        .verifyComplete();

    StepVerifier.create(pizzaScoreService.getForUser(userId, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.score()).isEqualTo(5))
        .verifyComplete();
  }

  @Test
  void getForUserReturnsEmptyWhenNoScoresExist() {
    UUID userId = UUID.randomUUID();

    StepVerifier.create(pizzaScoreService.getForUser(userId, DEFAULT_PIZZERIA_ID)).verifyComplete();
  }

  // Cross-tenant isolation tests

  @Test
  void getForUserReturnsOnlyScoresFromRequestedPizzeria() {
    UUID userId = UUID.randomUUID();
    UUID pizzaOneId = UUID.randomUUID();
    UUID pizzaTwoId = UUID.randomUUID();

    // Create score in DEFAULT_PIZZERIA
    StepVerifier.create(
            pizzaScoreService.create(
                userId,
                DEFAULT_PIZZERIA_ID,
                new PizzaScoreCreateRequest(pizzaOneId, PizzaType.TEMPLATE, 5, "Great at A!")))
        .assertNext(response -> assertThat(response.score()).isEqualTo(5))
        .verifyComplete();

    // Create score in OTHER_PIZZERIA
    StepVerifier.create(
            pizzaScoreService.create(
                userId,
                OTHER_PIZZERIA_ID,
                new PizzaScoreCreateRequest(pizzaTwoId, PizzaType.CUSTOM, 3, "OK at B")))
        .assertNext(response -> assertThat(response.score()).isEqualTo(3))
        .verifyComplete();

    // Get scores from DEFAULT_PIZZERIA should only return the score from that pizzeria
    StepVerifier.create(pizzaScoreService.getForUser(userId, DEFAULT_PIZZERIA_ID).collectList())
        .assertNext(
            scores -> {
              assertThat(scores).hasSize(1);
              assertThat(scores.get(0).pizzaId()).isEqualTo(pizzaOneId);
              assertThat(scores.get(0).comment()).isEqualTo("Great at A!");
            })
        .verifyComplete();

    // Get scores from OTHER_PIZZERIA should only return the score from that pizzeria
    StepVerifier.create(pizzaScoreService.getForUser(userId, OTHER_PIZZERIA_ID).collectList())
        .assertNext(
            scores -> {
              assertThat(scores).hasSize(1);
              assertThat(scores.get(0).pizzaId()).isEqualTo(pizzaTwoId);
              assertThat(scores.get(0).comment()).isEqualTo("OK at B");
            })
        .verifyComplete();
  }

  @Test
  void sameUserCanHaveScoresInDifferentPizzerias() {
    UUID userId = UUID.randomUUID();
    UUID pizzaId = UUID.randomUUID();

    // Same user rates the same pizza at two different pizzerias (different franchises)
    StepVerifier.create(
            pizzaScoreService.create(
                userId,
                DEFAULT_PIZZERIA_ID,
                new PizzaScoreCreateRequest(pizzaId, PizzaType.TEMPLATE, 5, "Perfect!")))
        .assertNext(response -> assertThat(response.score()).isEqualTo(5))
        .verifyComplete();

    StepVerifier.create(
            pizzaScoreService.create(
                userId,
                OTHER_PIZZERIA_ID,
                new PizzaScoreCreateRequest(pizzaId, PizzaType.TEMPLATE, 2, "Not as good")))
        .assertNext(response -> assertThat(response.score()).isEqualTo(2))
        .verifyComplete();

    // Both scores exist independently
    StepVerifier.create(pizzaScoreService.getForUser(userId, DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              assertThat(response.score()).isEqualTo(5);
              assertThat(response.comment()).isEqualTo("Perfect!");
            })
        .verifyComplete();

    StepVerifier.create(pizzaScoreService.getForUser(userId, OTHER_PIZZERIA_ID))
        .assertNext(
            response -> {
              assertThat(response.score()).isEqualTo(2);
              assertThat(response.comment()).isEqualTo("Not as good");
            })
        .verifyComplete();
  }

  @Test
  void createStoresCorrectPizzeriaId() {
    UUID userId = UUID.randomUUID();
    UUID pizzaId = UUID.randomUUID();

    StepVerifier.create(
            pizzaScoreService.create(
                userId,
                DEFAULT_PIZZERIA_ID,
                new PizzaScoreCreateRequest(pizzaId, PizzaType.TEMPLATE, 4, "Good")))
        .assertNext(
            response -> {
              // Verify score is retrievable only from correct pizzeria
              assertThat(response.userId()).isEqualTo(userId);
            })
        .verifyComplete();

    // Score should be found in DEFAULT_PIZZERIA
    StepVerifier.create(pizzaScoreService.getForUser(userId, DEFAULT_PIZZERIA_ID).count())
        .expectNext(1L)
        .verifyComplete();

    // Score should NOT be found in OTHER_PIZZERIA
    StepVerifier.create(pizzaScoreService.getForUser(userId, OTHER_PIZZERIA_ID).count())
        .expectNext(0L)
        .verifyComplete();

    // Verify createdBy audit field is set
    PizzaScore stored =
        repository.findByUserIdAndPizzeriaId(userId, DEFAULT_PIZZERIA_ID).blockFirst();
    assertThat(stored).isNotNull();
    assertThat(stored.createdBy()).isEqualTo(userId);
  }

  private static final class TestTimeProvider implements TimeProvider {
    private final Instant instant;

    private TestTimeProvider(Instant instant) {
      this.instant = instant;
    }

    @Override
    public Instant now() {
      return instant;
    }
  }
}
