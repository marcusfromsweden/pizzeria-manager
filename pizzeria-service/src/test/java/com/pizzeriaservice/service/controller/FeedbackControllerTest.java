package com.pizzeriaservice.service.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.api.dto.FeedbackResponse;
import com.pizzeriaservice.api.dto.ServiceFeedbackRequest;
import com.pizzeriaservice.service.service.FeedbackService;
import com.pizzeriaservice.service.support.security.AuthenticatedUser;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class FeedbackControllerTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private FeedbackService feedbackService;

  private FeedbackController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new FeedbackController(feedbackService);
  }

  @Test
  void submitServiceFeedbackDelegatesToService() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);
    ServiceFeedbackRequest request = new ServiceFeedbackRequest("Great", 5, "DELIVERY");
    FeedbackResponse response =
        new FeedbackResponse(
            UUID.randomUUID(),
            userId,
            "SERVICE",
            "Great",
            5,
            "DELIVERY",
            null,
            null,
            null,
            Instant.parse("2024-01-01T00:00:00Z"));

    when(feedbackService.submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request))
        .thenReturn(Mono.just(response));

    StepVerifier.create(controller.submitServiceFeedback(user, request))
        .expectNext(response)
        .verifyComplete();

    verify(feedbackService).submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request);
  }
}
