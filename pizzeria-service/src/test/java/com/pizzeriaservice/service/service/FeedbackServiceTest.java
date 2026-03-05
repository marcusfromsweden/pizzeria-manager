package com.pizzeriaservice.service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pizzeriaservice.api.dto.ServiceFeedbackRequest;
import com.pizzeriaservice.service.converter.RestDomainConverter;
import com.pizzeriaservice.service.domain.model.Feedback;
import com.pizzeriaservice.service.repository.inmemory.InMemoryFeedbackRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class FeedbackServiceTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID OTHER_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000002");

  private FeedbackService feedbackService;
  private InMemoryFeedbackRepository feedbackRepository;

  @BeforeEach
  void setUp() {
    feedbackRepository = new InMemoryFeedbackRepository();
    feedbackService =
        new FeedbackService(
            feedbackRepository,
            () -> Instant.parse("2024-01-01T00:00:00Z"),
            new RestDomainConverter());
  }

  @Test
  void submitServiceFeedbackPersistsEntry() {
    ServiceFeedbackRequest request = new ServiceFeedbackRequest("Great service", 5, "DELIVERY");
    UUID userId = UUID.randomUUID();

    StepVerifier.create(feedbackService.submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request))
        .assertNext(
            response -> {
              assertThat(response.userId()).isEqualTo(userId);
              assertThat(response.type()).isEqualTo("SERVICE");
              assertThat(response.message()).isEqualTo("Great service");
              assertThat(response.rating()).isEqualTo(5);
              assertThat(response.createdAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
            })
        .verifyComplete();
  }

  // Cross-tenant isolation tests

  @Test
  void submitServiceFeedbackStoresCorrectPizzeriaId() {
    ServiceFeedbackRequest request = new ServiceFeedbackRequest("Great pizza", 5, "DINE_IN");
    UUID userId = UUID.randomUUID();

    StepVerifier.create(feedbackService.submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request))
        .assertNext(
            response -> {
              // Verify the stored feedback has correct pizzeriaId
              Feedback stored = feedbackRepository.findById(response.id());
              assertThat(stored).isNotNull();
              assertThat(stored.pizzeriaId()).isEqualTo(DEFAULT_PIZZERIA_ID);
              assertThat(stored.userId()).isEqualTo(userId);
              // Verify audit fields
              assertThat(stored.createdBy()).isEqualTo(userId);
              assertThat(stored.updatedBy()).isEqualTo(userId);
            })
        .verifyComplete();
  }

  @Test
  void feedbackFromDifferentPizzeriasAreIsolated() {
    UUID userOneId = UUID.randomUUID();
    UUID userTwoId = UUID.randomUUID();

    ServiceFeedbackRequest requestOne =
        new ServiceFeedbackRequest("Great at pizzeria A", 5, "DELIVERY");
    ServiceFeedbackRequest requestTwo =
        new ServiceFeedbackRequest("OK at pizzeria B", 3, "DINE_IN");

    // Submit feedback to DEFAULT_PIZZERIA
    StepVerifier.create(
            feedbackService.submitServiceFeedback(userOneId, DEFAULT_PIZZERIA_ID, requestOne))
        .assertNext(response -> assertThat(response.message()).isEqualTo("Great at pizzeria A"))
        .verifyComplete();

    // Submit feedback to OTHER_PIZZERIA
    StepVerifier.create(
            feedbackService.submitServiceFeedback(userTwoId, OTHER_PIZZERIA_ID, requestTwo))
        .assertNext(response -> assertThat(response.message()).isEqualTo("OK at pizzeria B"))
        .verifyComplete();

    // Verify both feedbacks are stored with correct pizzeriaIds
    Map<UUID, Feedback> allFeedback = feedbackRepository.findAll();
    assertThat(allFeedback).hasSize(2);

    long countDefaultPizzeria =
        allFeedback.values().stream()
            .filter(f -> DEFAULT_PIZZERIA_ID.equals(f.pizzeriaId()))
            .count();
    long countOtherPizzeria =
        allFeedback.values().stream().filter(f -> OTHER_PIZZERIA_ID.equals(f.pizzeriaId())).count();

    assertThat(countDefaultPizzeria).isEqualTo(1);
    assertThat(countOtherPizzeria).isEqualTo(1);

    // Verify the correct user is associated with each pizzeria's feedback
    Feedback pizzeriaAFeedback =
        allFeedback.values().stream()
            .filter(f -> DEFAULT_PIZZERIA_ID.equals(f.pizzeriaId()))
            .findFirst()
            .orElseThrow();
    assertThat(pizzeriaAFeedback.userId()).isEqualTo(userOneId);
    assertThat(pizzeriaAFeedback.message()).isEqualTo("Great at pizzeria A");

    Feedback pizzeriaBFeedback =
        allFeedback.values().stream()
            .filter(f -> OTHER_PIZZERIA_ID.equals(f.pizzeriaId()))
            .findFirst()
            .orElseThrow();
    assertThat(pizzeriaBFeedback.userId()).isEqualTo(userTwoId);
    assertThat(pizzeriaBFeedback.message()).isEqualTo("OK at pizzeria B");
  }

  // Tests for getAllForPizzeria (admin feature)

  @Test
  void getAllForPizzeriaReturnsAllFeedbackForPizzeria() {
    UUID userOneId = UUID.randomUUID();
    UUID userTwoId = UUID.randomUUID();

    // Submit feedback from two different users to the same pizzeria
    ServiceFeedbackRequest request1 = new ServiceFeedbackRequest("Great service", 5, "Delivery");
    ServiceFeedbackRequest request2 = new ServiceFeedbackRequest("Good food", 4, "Dine-in");

    StepVerifier.create(
            feedbackService.submitServiceFeedback(userOneId, DEFAULT_PIZZERIA_ID, request1))
        .expectNextCount(1)
        .verifyComplete();

    StepVerifier.create(
            feedbackService.submitServiceFeedback(userTwoId, DEFAULT_PIZZERIA_ID, request2))
        .expectNextCount(1)
        .verifyComplete();

    // Verify getAllForPizzeria returns both (order not guaranteed with same timestamp)
    StepVerifier.create(feedbackService.getAllForPizzeria(DEFAULT_PIZZERIA_ID).collectList())
        .assertNext(
            responses -> {
              assertThat(responses).hasSize(2);
              assertThat(responses)
                  .extracting("message")
                  .containsExactlyInAnyOrder("Great service", "Good food");
            })
        .verifyComplete();
  }

  @Test
  void getAllForPizzeriaOnlyReturnsForSpecificPizzeria() {
    UUID userId = UUID.randomUUID();

    // Submit feedback to DEFAULT_PIZZERIA
    ServiceFeedbackRequest request1 =
        new ServiceFeedbackRequest("Feedback for default", 5, "Delivery");
    StepVerifier.create(
            feedbackService.submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request1))
        .expectNextCount(1)
        .verifyComplete();

    // Submit feedback to OTHER_PIZZERIA
    ServiceFeedbackRequest request2 =
        new ServiceFeedbackRequest("Feedback for other", 4, "Dine-in");
    StepVerifier.create(feedbackService.submitServiceFeedback(userId, OTHER_PIZZERIA_ID, request2))
        .expectNextCount(1)
        .verifyComplete();

    // Verify getAllForPizzeria only returns feedback for DEFAULT_PIZZERIA
    StepVerifier.create(feedbackService.getAllForPizzeria(DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              assertThat(response.message()).isEqualTo("Feedback for default");
            })
        .verifyComplete();

    // Verify getAllForPizzeria only returns feedback for OTHER_PIZZERIA
    StepVerifier.create(feedbackService.getAllForPizzeria(OTHER_PIZZERIA_ID))
        .assertNext(
            response -> {
              assertThat(response.message()).isEqualTo("Feedback for other");
            })
        .verifyComplete();
  }

  @Test
  void getAllForPizzeriaReturnsEmptyWhenNoFeedback() {
    StepVerifier.create(feedbackService.getAllForPizzeria(DEFAULT_PIZZERIA_ID)).verifyComplete();
  }

  @Test
  void getAllForPizzeriaIncludesCategory() {
    UUID userId = UUID.randomUUID();
    ServiceFeedbackRequest request = new ServiceFeedbackRequest("Great delivery", 5, "Delivery");

    StepVerifier.create(feedbackService.submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request))
        .expectNextCount(1)
        .verifyComplete();

    StepVerifier.create(feedbackService.getAllForPizzeria(DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              assertThat(response.message()).isEqualTo("Great delivery");
              assertThat(response.category()).isEqualTo("Delivery");
            })
        .verifyComplete();
  }

  // Tests for addAdminReply

  @Test
  void addAdminReplyUpdatesFeedback() {
    UUID userId = UUID.randomUUID();
    ServiceFeedbackRequest request = new ServiceFeedbackRequest("Great service", 5, "Delivery");

    // Submit feedback
    UUID feedbackId =
        feedbackService.submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request).block().id();

    // Add admin reply
    StepVerifier.create(
            feedbackService.addAdminReply(
                feedbackId, DEFAULT_PIZZERIA_ID, "Thank you for your feedback!"))
        .assertNext(
            response -> {
              assertThat(response.id()).isEqualTo(feedbackId);
              assertThat(response.adminReply()).isEqualTo("Thank you for your feedback!");
              assertThat(response.adminRepliedAt())
                  .isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
            })
        .verifyComplete();

    // Verify persisted in storage
    Feedback stored = feedbackRepository.findById(feedbackId);
    assertThat(stored.adminReply()).isEqualTo("Thank you for your feedback!");
    assertThat(stored.adminRepliedAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
  }

  @Test
  void addAdminReplyFailsForNonExistentFeedback() {
    UUID nonExistentId = UUID.randomUUID();

    StepVerifier.create(
            feedbackService.addAdminReply(nonExistentId, DEFAULT_PIZZERIA_ID, "Reply to nothing"))
        .expectErrorMatches(
            throwable ->
                throwable instanceof IllegalArgumentException
                    && throwable.getMessage().contains("Feedback not found"))
        .verify();
  }

  @Test
  void addAdminReplyFailsForDifferentPizzeria() {
    UUID userId = UUID.randomUUID();
    ServiceFeedbackRequest request = new ServiceFeedbackRequest("Great service", 5, "Delivery");

    // Submit feedback to DEFAULT_PIZZERIA
    UUID feedbackId =
        feedbackService.submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request).block().id();

    // Try to add reply as OTHER_PIZZERIA (should fail)
    StepVerifier.create(
            feedbackService.addAdminReply(feedbackId, OTHER_PIZZERIA_ID, "Wrong pizzeria reply"))
        .expectErrorMatches(
            throwable ->
                throwable instanceof IllegalArgumentException
                    && throwable.getMessage().contains("Feedback not found"))
        .verify();

    // Verify original feedback unchanged
    Feedback stored = feedbackRepository.findById(feedbackId);
    assertThat(stored.adminReply()).isNull();
    assertThat(stored.adminRepliedAt()).isNull();
  }

  @Test
  void addAdminReplyPreservesOriginalFeedbackData() {
    UUID userId = UUID.randomUUID();
    ServiceFeedbackRequest request = new ServiceFeedbackRequest("Great service", 5, "Delivery");

    // Submit feedback
    UUID feedbackId =
        feedbackService.submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request).block().id();

    // Add admin reply
    StepVerifier.create(feedbackService.addAdminReply(feedbackId, DEFAULT_PIZZERIA_ID, "Thanks!"))
        .assertNext(
            response -> {
              // Verify original data preserved
              assertThat(response.userId()).isEqualTo(userId);
              assertThat(response.message()).isEqualTo("Great service");
              assertThat(response.rating()).isEqualTo(5);
              assertThat(response.category()).isEqualTo("Delivery");
              assertThat(response.type()).isEqualTo("SERVICE");
              // Verify reply added
              assertThat(response.adminReply()).isEqualTo("Thanks!");
            })
        .verifyComplete();
  }

  // Tests for getUnreadReplyCount

  @Test
  void getUnreadReplyCountReturnsZeroWhenNoFeedback() {
    UUID userId = UUID.randomUUID();

    StepVerifier.create(feedbackService.getUnreadReplyCount(userId, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.unreadCount()).isEqualTo(0))
        .verifyComplete();
  }

  @Test
  void getUnreadReplyCountReturnsZeroWhenNoReplies() {
    UUID userId = UUID.randomUUID();
    ServiceFeedbackRequest request = new ServiceFeedbackRequest("Great service", 5, "Delivery");

    // Submit feedback without admin reply
    feedbackService.submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request).block();

    StepVerifier.create(feedbackService.getUnreadReplyCount(userId, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.unreadCount()).isEqualTo(0))
        .verifyComplete();
  }

  @Test
  void getUnreadReplyCountReturnsCountOfUnreadReplies() {
    UUID userId = UUID.randomUUID();

    // Submit two feedbacks and add replies
    ServiceFeedbackRequest request1 = new ServiceFeedbackRequest("First feedback", 5, "Delivery");
    ServiceFeedbackRequest request2 = new ServiceFeedbackRequest("Second feedback", 4, "Dine-in");

    UUID feedbackId1 =
        feedbackService.submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request1).block().id();
    UUID feedbackId2 =
        feedbackService.submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request2).block().id();

    // Add admin replies to both
    feedbackService
        .addAdminReply(feedbackId1, DEFAULT_PIZZERIA_ID, "Thanks for feedback 1!")
        .block();
    feedbackService
        .addAdminReply(feedbackId2, DEFAULT_PIZZERIA_ID, "Thanks for feedback 2!")
        .block();

    StepVerifier.create(feedbackService.getUnreadReplyCount(userId, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.unreadCount()).isEqualTo(2))
        .verifyComplete();
  }

  @Test
  void getUnreadReplyCountOnlyCountsForSpecificUserAndPizzeria() {
    UUID userOneId = UUID.randomUUID();
    UUID userTwoId = UUID.randomUUID();

    // User one submits to DEFAULT_PIZZERIA
    ServiceFeedbackRequest request1 =
        new ServiceFeedbackRequest("User one feedback", 5, "Delivery");
    UUID feedbackId1 =
        feedbackService
            .submitServiceFeedback(userOneId, DEFAULT_PIZZERIA_ID, request1)
            .block()
            .id();

    // User two submits to DEFAULT_PIZZERIA
    ServiceFeedbackRequest request2 = new ServiceFeedbackRequest("User two feedback", 4, "Dine-in");
    UUID feedbackId2 =
        feedbackService
            .submitServiceFeedback(userTwoId, DEFAULT_PIZZERIA_ID, request2)
            .block()
            .id();

    // User one submits to OTHER_PIZZERIA
    ServiceFeedbackRequest request3 =
        new ServiceFeedbackRequest("User one other pizzeria", 3, "Delivery");
    UUID feedbackId3 =
        feedbackService.submitServiceFeedback(userOneId, OTHER_PIZZERIA_ID, request3).block().id();

    // Add replies to all
    feedbackService.addAdminReply(feedbackId1, DEFAULT_PIZZERIA_ID, "Reply 1").block();
    feedbackService.addAdminReply(feedbackId2, DEFAULT_PIZZERIA_ID, "Reply 2").block();
    feedbackService.addAdminReply(feedbackId3, OTHER_PIZZERIA_ID, "Reply 3").block();

    // User one at DEFAULT_PIZZERIA should have 1 unread
    StepVerifier.create(feedbackService.getUnreadReplyCount(userOneId, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.unreadCount()).isEqualTo(1))
        .verifyComplete();

    // User two at DEFAULT_PIZZERIA should have 1 unread
    StepVerifier.create(feedbackService.getUnreadReplyCount(userTwoId, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.unreadCount()).isEqualTo(1))
        .verifyComplete();

    // User one at OTHER_PIZZERIA should have 1 unread
    StepVerifier.create(feedbackService.getUnreadReplyCount(userOneId, OTHER_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.unreadCount()).isEqualTo(1))
        .verifyComplete();
  }

  // Tests for markAllRepliesAsRead

  @Test
  void markAllRepliesAsReadMarksUnreadReplies() {
    UUID userId = UUID.randomUUID();

    // Submit feedback and add reply
    ServiceFeedbackRequest request = new ServiceFeedbackRequest("Great service", 5, "Delivery");
    UUID feedbackId =
        feedbackService.submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request).block().id();
    feedbackService.addAdminReply(feedbackId, DEFAULT_PIZZERIA_ID, "Thanks!").block();

    // Verify initially unread
    StepVerifier.create(feedbackService.getUnreadReplyCount(userId, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.unreadCount()).isEqualTo(1))
        .verifyComplete();

    // Mark as read
    StepVerifier.create(feedbackService.markAllRepliesAsRead(userId, DEFAULT_PIZZERIA_ID))
        .verifyComplete();

    // Verify now zero unread
    StepVerifier.create(feedbackService.getUnreadReplyCount(userId, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.unreadCount()).isEqualTo(0))
        .verifyComplete();

    // Verify the feedback has adminReplyReadAt set
    Feedback stored = feedbackRepository.findById(feedbackId);
    assertThat(stored.adminReplyReadAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
  }

  @Test
  void markAllRepliesAsReadOnlyAffectsSpecificUserAndPizzeria() {
    UUID userOneId = UUID.randomUUID();
    UUID userTwoId = UUID.randomUUID();

    // User one submits to DEFAULT_PIZZERIA
    ServiceFeedbackRequest request1 =
        new ServiceFeedbackRequest("User one feedback", 5, "Delivery");
    UUID feedbackId1 =
        feedbackService
            .submitServiceFeedback(userOneId, DEFAULT_PIZZERIA_ID, request1)
            .block()
            .id();

    // User two submits to DEFAULT_PIZZERIA
    ServiceFeedbackRequest request2 = new ServiceFeedbackRequest("User two feedback", 4, "Dine-in");
    UUID feedbackId2 =
        feedbackService
            .submitServiceFeedback(userTwoId, DEFAULT_PIZZERIA_ID, request2)
            .block()
            .id();

    // Add replies to both
    feedbackService.addAdminReply(feedbackId1, DEFAULT_PIZZERIA_ID, "Reply 1").block();
    feedbackService.addAdminReply(feedbackId2, DEFAULT_PIZZERIA_ID, "Reply 2").block();

    // Mark user one's replies as read
    feedbackService.markAllRepliesAsRead(userOneId, DEFAULT_PIZZERIA_ID).block();

    // User one should have 0 unread
    StepVerifier.create(feedbackService.getUnreadReplyCount(userOneId, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.unreadCount()).isEqualTo(0))
        .verifyComplete();

    // User two should still have 1 unread
    StepVerifier.create(feedbackService.getUnreadReplyCount(userTwoId, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.unreadCount()).isEqualTo(1))
        .verifyComplete();
  }

  @Test
  void markAllRepliesAsReadDoesNotAffectFeedbackWithoutReplies() {
    UUID userId = UUID.randomUUID();

    // Submit feedback without reply
    ServiceFeedbackRequest request = new ServiceFeedbackRequest("No reply yet", 5, "Delivery");
    UUID feedbackId =
        feedbackService.submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request).block().id();

    // Mark as read (should not error, just do nothing)
    StepVerifier.create(feedbackService.markAllRepliesAsRead(userId, DEFAULT_PIZZERIA_ID))
        .verifyComplete();

    // Feedback should not have adminReplyReadAt set
    Feedback stored = feedbackRepository.findById(feedbackId);
    assertThat(stored.adminReplyReadAt()).isNull();
  }

  @Test
  void addAdminReplyClearsReadAtTimestamp() {
    UUID userId = UUID.randomUUID();

    // Submit feedback, add reply, and mark as read
    ServiceFeedbackRequest request = new ServiceFeedbackRequest("Great service", 5, "Delivery");
    UUID feedbackId =
        feedbackService.submitServiceFeedback(userId, DEFAULT_PIZZERIA_ID, request).block().id();
    feedbackService.addAdminReply(feedbackId, DEFAULT_PIZZERIA_ID, "First reply").block();
    feedbackService.markAllRepliesAsRead(userId, DEFAULT_PIZZERIA_ID).block();

    // Verify read
    StepVerifier.create(feedbackService.getUnreadReplyCount(userId, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.unreadCount()).isEqualTo(0))
        .verifyComplete();

    // Add another reply (should reset read status)
    feedbackService.addAdminReply(feedbackId, DEFAULT_PIZZERIA_ID, "Updated reply").block();

    // Should be unread again
    StepVerifier.create(feedbackService.getUnreadReplyCount(userId, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.unreadCount()).isEqualTo(1))
        .verifyComplete();

    Feedback stored = feedbackRepository.findById(feedbackId);
    assertThat(stored.adminReply()).isEqualTo("Updated reply");
    assertThat(stored.adminReplyReadAt()).isNull();
  }
}
