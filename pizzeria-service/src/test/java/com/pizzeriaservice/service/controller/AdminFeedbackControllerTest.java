package com.pizzeriaservice.service.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.api.dto.AdminReplyRequest;
import com.pizzeriaservice.api.dto.FeedbackResponse;
import com.pizzeriaservice.service.service.FeedbackService;
import com.pizzeriaservice.service.service.PizzeriaService;
import com.pizzeriaservice.service.support.ForbiddenException;
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

class AdminFeedbackControllerTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final String PIZZERIA_CODE = "testpizzeria";

  @Mock private FeedbackService feedbackService;
  @Mock private PizzeriaService pizzeriaService;

  private AdminFeedbackController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new AdminFeedbackController(feedbackService, pizzeriaService);
  }

  @Test
  void getAllFeedbackReturnsAllFeedbackForPizzeria() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser adminUser = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, PIZZERIA_CODE);

    FeedbackResponse feedback1 =
        new FeedbackResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "SERVICE",
            "Great service",
            5,
            "Delivery",
            null,
            null,
            null,
            Instant.parse("2024-01-01T00:00:00Z"));
    FeedbackResponse feedback2 =
        new FeedbackResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "SERVICE",
            "Good food",
            4,
            null,
            null,
            null,
            null,
            Instant.parse("2024-01-02T00:00:00Z"));

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(feedbackService.getAllForPizzeria(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.just(feedback1, feedback2));

    StepVerifier.create(controller.getAllFeedback(PIZZERIA_CODE, adminUser))
        .expectNext(feedback1)
        .expectNext(feedback2)
        .verifyComplete();

    verify(pizzeriaService).resolvePizzeriaId(PIZZERIA_CODE);
    verify(feedbackService).getAllForPizzeria(DEFAULT_PIZZERIA_ID);
  }

  @Test
  void getAllFeedbackThrowsForbiddenWhenNotAdmin() {
    UUID userId = UUID.randomUUID();
    // User is not admin for this pizzeria (adminFor is null)
    AuthenticatedUser nonAdminUser = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);

    assertThatThrownBy(() -> controller.getAllFeedback(PIZZERIA_CODE, nonAdminUser))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("not authorized");
  }

  @Test
  void getAllFeedbackThrowsForbiddenWhenAdminForDifferentPizzeria() {
    UUID userId = UUID.randomUUID();
    // User is admin for a different pizzeria
    AuthenticatedUser wrongPizzeriaAdmin =
        new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, "otherpizzeria");

    assertThatThrownBy(() -> controller.getAllFeedback(PIZZERIA_CODE, wrongPizzeriaAdmin))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("not authorized");
  }

  @Test
  void getAllFeedbackReturnsEmptyWhenNoFeedback() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser adminUser = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, PIZZERIA_CODE);

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(feedbackService.getAllForPizzeria(DEFAULT_PIZZERIA_ID)).thenReturn(Flux.empty());

    StepVerifier.create(controller.getAllFeedback(PIZZERIA_CODE, adminUser)).verifyComplete();
  }

  // Tests for replyToFeedback endpoint

  @Test
  void replyToFeedbackSucceeds() {
    UUID userId = UUID.randomUUID();
    UUID feedbackId = UUID.randomUUID();
    AuthenticatedUser adminUser = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, PIZZERIA_CODE);
    AdminReplyRequest request = new AdminReplyRequest("Thank you for your feedback!");

    FeedbackResponse response =
        new FeedbackResponse(
            feedbackId,
            UUID.randomUUID(),
            "SERVICE",
            "Great service",
            5,
            "Delivery",
            "Thank you for your feedback!",
            Instant.parse("2024-01-01T00:00:00Z"),
            null,
            Instant.parse("2024-01-01T00:00:00Z"));

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(feedbackService.addAdminReply(
            feedbackId, DEFAULT_PIZZERIA_ID, "Thank you for your feedback!"))
        .thenReturn(Mono.just(response));

    StepVerifier.create(controller.replyToFeedback(PIZZERIA_CODE, feedbackId, request, adminUser))
        .expectNext(response)
        .verifyComplete();

    verify(pizzeriaService).resolvePizzeriaId(PIZZERIA_CODE);
    verify(feedbackService)
        .addAdminReply(feedbackId, DEFAULT_PIZZERIA_ID, "Thank you for your feedback!");
  }

  @Test
  void replyToFeedbackThrowsForbiddenWhenNotAdmin() {
    UUID userId = UUID.randomUUID();
    UUID feedbackId = UUID.randomUUID();
    AuthenticatedUser nonAdminUser = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);
    AdminReplyRequest request = new AdminReplyRequest("Should not work");

    assertThatThrownBy(
            () -> controller.replyToFeedback(PIZZERIA_CODE, feedbackId, request, nonAdminUser))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("not authorized");
  }

  @Test
  void replyToFeedbackThrowsForbiddenWhenAdminForDifferentPizzeria() {
    UUID userId = UUID.randomUUID();
    UUID feedbackId = UUID.randomUUID();
    AuthenticatedUser wrongPizzeriaAdmin =
        new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, "otherpizzeria");
    AdminReplyRequest request = new AdminReplyRequest("Wrong pizzeria");

    assertThatThrownBy(
            () ->
                controller.replyToFeedback(PIZZERIA_CODE, feedbackId, request, wrongPizzeriaAdmin))
        .isInstanceOf(ForbiddenException.class)
        .hasMessageContaining("not authorized");
  }
}
