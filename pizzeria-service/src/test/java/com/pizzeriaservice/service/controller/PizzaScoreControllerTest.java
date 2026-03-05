package com.pizzeriaservice.service.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.api.dto.PizzaScoreCreateRequest;
import com.pizzeriaservice.api.dto.PizzaScoreResponse;
import com.pizzeriaservice.api.dto.PizzaType;
import com.pizzeriaservice.service.service.PizzaScoreService;
import com.pizzeriaservice.service.support.security.AuthenticatedUser;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PizzaScoreControllerTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private PizzaScoreService pizzaScoreService;

  private PizzaScoreController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new PizzaScoreController(pizzaScoreService);
  }

  @Test
  void createScoreDelegatesToService() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);
    UUID pizzaId = UUID.randomUUID();
    PizzaScoreCreateRequest request =
        new PizzaScoreCreateRequest(pizzaId, PizzaType.CUSTOM, 4, "Nice");
    PizzaScoreResponse response =
        new PizzaScoreResponse(
            UUID.randomUUID(),
            userId,
            pizzaId,
            PizzaType.CUSTOM,
            4,
            "Nice",
            Instant.parse("2024-01-01T00:00:00Z"));

    when(pizzaScoreService.create(userId, DEFAULT_PIZZERIA_ID, request))
        .thenReturn(Mono.just(response));

    StepVerifier.create(controller.createScore(user, request))
        .expectNext(response)
        .verifyComplete();

    verify(pizzaScoreService).create(userId, DEFAULT_PIZZERIA_ID, request);
  }

  @Test
  void myScoresReturnsServiceFlux() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);
    PizzaScoreResponse response =
        new PizzaScoreResponse(
            UUID.randomUUID(),
            userId,
            UUID.randomUUID(),
            PizzaType.TEMPLATE,
            5,
            "Great",
            Instant.parse("2024-01-02T00:00:00Z"));

    when(pizzaScoreService.getForUser(userId, DEFAULT_PIZZERIA_ID)).thenReturn(Flux.just(response));

    StepVerifier.create(controller.myScores(user)).expectNext(response).verifyComplete();

    verify(pizzaScoreService).getForUser(userId, DEFAULT_PIZZERIA_ID);
  }
}
