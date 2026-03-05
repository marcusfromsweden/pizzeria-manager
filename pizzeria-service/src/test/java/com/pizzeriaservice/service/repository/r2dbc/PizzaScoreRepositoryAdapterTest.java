package com.pizzeriaservice.service.repository.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.service.domain.PizzaKind;
import com.pizzeriaservice.service.domain.model.PizzaScore;
import com.pizzeriaservice.service.pizzascore.PizzaScoreEntity;
import com.pizzeriaservice.service.pizzascore.PizzaScoreRepositoryR2dbc;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PizzaScoreRepositoryAdapterTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private PizzaScoreRepositoryR2dbc r2dbcRepository;
  private PizzaScoreRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    adapter = new PizzaScoreRepositoryAdapter(r2dbcRepository);
  }

  @Test
  void saveTranslatesBetweenDomainAndEntity() {
    UUID scoreId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID pizzaId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2024-03-03T12:00:00Z");
    PizzaScore score =
        PizzaScore.builder()
            .id(scoreId)
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .userId(userId)
            .pizzaId(pizzaId)
            .pizzaKind(PizzaKind.CUSTOM)
            .score(4)
            .comment("Nice one")
            .createdAt(createdAt)
            .build();

    PizzaScoreEntity entity =
        new PizzaScoreEntity(
            scoreId,
            DEFAULT_PIZZERIA_ID,
            userId,
            pizzaId,
            "CUSTOM",
            4,
            "Nice one",
            createdAt,
            null,
            false);
    when(r2dbcRepository.save(any(PizzaScoreEntity.class))).thenReturn(Mono.just(entity));

    StepVerifier.create(adapter.save(score))
        .assertNext(
            saved -> {
              assertThat(saved.id()).isEqualTo(scoreId);
              assertThat(saved.pizzeriaId()).isEqualTo(DEFAULT_PIZZERIA_ID);
              assertThat(saved.userId()).isEqualTo(userId);
              assertThat(saved.pizzaId()).isEqualTo(pizzaId);
              assertThat(saved.pizzaKind()).isEqualTo(PizzaKind.CUSTOM);
              assertThat(saved.score()).isEqualTo(4);
              assertThat(saved.comment()).isEqualTo("Nice one");
              assertThat(saved.createdAt()).isEqualTo(createdAt);
            })
        .verifyComplete();

    ArgumentCaptor<PizzaScoreEntity> entityCaptor = ArgumentCaptor.forClass(PizzaScoreEntity.class);
    verify(r2dbcRepository).save(entityCaptor.capture());
    PizzaScoreEntity captured = entityCaptor.getValue();
    assertThat(captured.getPizzeriaId()).isEqualTo(DEFAULT_PIZZERIA_ID);
    assertThat(captured.getPizzaKind()).isEqualTo("CUSTOM");
    assertThat(captured.getScore()).isEqualTo(4);
    assertThat(captured.getComment()).isEqualTo("Nice one");
  }

  @Test
  void findByUserIdMapsEntitiesToDomain() {
    UUID userId = UUID.randomUUID();
    UUID scoreId = UUID.randomUUID();
    UUID pizzaId = UUID.randomUUID();

    PizzaScoreEntity entity =
        new PizzaScoreEntity(
            scoreId,
            DEFAULT_PIZZERIA_ID,
            userId,
            pizzaId,
            "TEMPLATE",
            5,
            "Top tier",
            Instant.parse("2024-03-05T08:45:00Z"),
            null,
            false);
    when(r2dbcRepository.findAllByUserId(userId)).thenReturn(Flux.just(entity));

    StepVerifier.create(adapter.findByUserId(userId).collectList())
        .assertNext(
            scores -> {
              assertThat(scores).hasSize(1);
              PizzaScore returned = scores.get(0);
              assertThat(returned.id()).isEqualTo(scoreId);
              assertThat(returned.pizzeriaId()).isEqualTo(DEFAULT_PIZZERIA_ID);
              assertThat(returned.userId()).isEqualTo(userId);
              assertThat(returned.pizzaId()).isEqualTo(pizzaId);
              assertThat(returned.pizzaKind()).isEqualTo(PizzaKind.TEMPLATE);
              assertThat(returned.score()).isEqualTo(5);
              assertThat(returned.comment()).isEqualTo("Top tier");
              assertThat(returned.createdAt()).isEqualTo(Instant.parse("2024-03-05T08:45:00Z"));
            })
        .verifyComplete();
  }
}
