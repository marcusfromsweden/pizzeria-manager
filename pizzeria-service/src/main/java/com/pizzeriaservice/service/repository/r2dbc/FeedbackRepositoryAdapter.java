package com.pizzeriaservice.service.repository.r2dbc;

import com.pizzeriaservice.service.domain.FeedbackKind;
import com.pizzeriaservice.service.domain.FeedbackStatus;
import com.pizzeriaservice.service.domain.model.Feedback;
import com.pizzeriaservice.service.feedback.FeedbackEntity;
import com.pizzeriaservice.service.feedback.FeedbackRepositoryR2dbc;
import com.pizzeriaservice.service.repository.FeedbackRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class FeedbackRepositoryAdapter implements FeedbackRepository {

  private final FeedbackRepositoryR2dbc repository;

  @Override
  public Mono<Feedback> save(Feedback feedback) {
    FeedbackEntity entity = toEntity(feedback);
    return repository.save(entity).map(FeedbackRepositoryAdapter::toDomain);
  }

  @Override
  public Flux<Feedback> findByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId) {
    return repository
        .findAllByUserIdAndPizzeriaIdOrderByCreatedAtDesc(userId, pizzeriaId)
        .map(FeedbackRepositoryAdapter::toDomain);
  }

  @Override
  public Flux<Feedback> findByPizzeriaId(UUID pizzeriaId) {
    return repository
        .findAllByPizzeriaIdOrderByCreatedAtDesc(pizzeriaId)
        .map(FeedbackRepositoryAdapter::toDomain);
  }

  @Override
  public Mono<Feedback> findByIdAndPizzeriaId(UUID id, UUID pizzeriaId) {
    return repository
        .findByIdAndPizzeriaId(id, pizzeriaId)
        .map(FeedbackRepositoryAdapter::toDomain);
  }

  @Override
  public Mono<Long> countUnreadRepliesByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId) {
    return repository.countUnreadRepliesByUserIdAndPizzeriaId(userId, pizzeriaId);
  }

  @Override
  public Mono<Void> markAllRepliesAsReadByUserIdAndPizzeriaId(
      UUID userId, UUID pizzeriaId, Instant readAt) {
    return repository.markAllRepliesAsReadByUserIdAndPizzeriaId(userId, pizzeriaId, readAt).then();
  }

  private static FeedbackEntity toEntity(Feedback feedback) {
    if (feedback.isNew()) {
      return FeedbackEntity.builder()
          .id(feedback.id())
          .pizzeriaId(feedback.pizzeriaId())
          .userId(feedback.userId())
          .kind(feedback.kind().name())
          .message(feedback.message())
          .rating(feedback.rating())
          .category(feedback.category())
          .status(feedback.status().name())
          .adminReply(feedback.adminReply())
          .adminRepliedAt(feedback.adminRepliedAt())
          .adminReplyReadAt(feedback.adminReplyReadAt())
          .createdAt(feedback.createdAt())
          .updatedAt(feedback.updatedAt())
          .createdBy(feedback.createdBy())
          .updatedBy(feedback.updatedBy())
          .isNew(true)
          .build();
    }
    return FeedbackEntity.builder()
        .id(feedback.id())
        .pizzeriaId(feedback.pizzeriaId())
        .userId(feedback.userId())
        .kind(feedback.kind().name())
        .message(feedback.message())
        .rating(feedback.rating())
        .category(feedback.category())
        .status(feedback.status().name())
        .adminReply(feedback.adminReply())
        .adminRepliedAt(feedback.adminRepliedAt())
        .adminReplyReadAt(feedback.adminReplyReadAt())
        .createdAt(feedback.createdAt())
        .updatedAt(feedback.updatedAt())
        .createdBy(feedback.createdBy())
        .updatedBy(feedback.updatedBy())
        .build();
  }

  private static Feedback toDomain(FeedbackEntity entity) {
    return Feedback.builder()
        .id(entity.getId())
        .pizzeriaId(entity.getPizzeriaId())
        .userId(entity.getUserId())
        .kind(FeedbackKind.valueOf(entity.getKind()))
        .message(entity.getMessage())
        .rating(entity.getRating())
        .category(entity.getCategory())
        .status(FeedbackStatus.valueOf(entity.getStatus()))
        .adminReply(entity.getAdminReply())
        .adminRepliedAt(entity.getAdminRepliedAt())
        .adminReplyReadAt(entity.getAdminReplyReadAt())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .createdBy(entity.getCreatedBy())
        .updatedBy(entity.getUpdatedBy())
        .isNew(false)
        .build();
  }
}
