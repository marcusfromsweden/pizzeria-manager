package com.pizzeriaservice.service.service;

import com.pizzeriaservice.api.dto.FeedbackResponse;
import com.pizzeriaservice.api.dto.ServiceFeedbackRequest;
import com.pizzeriaservice.api.dto.UnreadFeedbackCountResponse;
import com.pizzeriaservice.service.converter.RestDomainConverter;
import com.pizzeriaservice.service.domain.FeedbackKind;
import com.pizzeriaservice.service.domain.FeedbackStatus;
import com.pizzeriaservice.service.domain.model.Feedback;
import com.pizzeriaservice.service.repository.FeedbackRepository;
import com.pizzeriaservice.service.support.TimeProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class FeedbackService {

  private final FeedbackRepository feedbackRepository;
  private final TimeProvider timeProvider;
  private final RestDomainConverter converter;

  public Mono<FeedbackResponse> submitServiceFeedback(
      UUID userId, UUID pizzeriaId, ServiceFeedbackRequest request) {
    Feedback feedback =
        Feedback.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(pizzeriaId)
            .userId(userId)
            .kind(FeedbackKind.SERVICE)
            .message(request.message())
            .rating(request.rating())
            .category(request.category())
            .status(FeedbackStatus.OPEN)
            .adminReply(null)
            .adminRepliedAt(null)
            .adminReplyReadAt(null)
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .createdBy(userId)
            .updatedBy(userId)
            .isNew(true)
            .build();

    return feedbackRepository.save(feedback).map(converter::toFeedbackResponse);
  }

  public Flux<FeedbackResponse> getForUser(UUID userId, UUID pizzeriaId) {
    return feedbackRepository
        .findByUserIdAndPizzeriaId(userId, pizzeriaId)
        .map(converter::toFeedbackResponse);
  }

  public Flux<FeedbackResponse> getAllForPizzeria(UUID pizzeriaId) {
    return feedbackRepository.findByPizzeriaId(pizzeriaId).map(converter::toFeedbackResponse);
  }

  public Mono<FeedbackResponse> addAdminReply(UUID feedbackId, UUID pizzeriaId, String reply) {
    return feedbackRepository
        .findByIdAndPizzeriaId(feedbackId, pizzeriaId)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Feedback not found")))
        .flatMap(
            feedback -> {
              Feedback updated =
                  feedback.toBuilder()
                      .adminReply(reply)
                      .adminRepliedAt(timeProvider.now())
                      .adminReplyReadAt(null)
                      .updatedAt(timeProvider.now())
                      .isNew(false)
                      .build();
              return feedbackRepository.save(updated);
            })
        .map(converter::toFeedbackResponse);
  }

  public Mono<UnreadFeedbackCountResponse> getUnreadReplyCount(UUID userId, UUID pizzeriaId) {
    return feedbackRepository
        .countUnreadRepliesByUserIdAndPizzeriaId(userId, pizzeriaId)
        .map(count -> new UnreadFeedbackCountResponse(count.intValue()));
  }

  public Mono<Void> markAllRepliesAsRead(UUID userId, UUID pizzeriaId) {
    return feedbackRepository.markAllRepliesAsReadByUserIdAndPizzeriaId(
        userId, pizzeriaId, timeProvider.now());
  }
}
