package com.pizzeriaservice.service.feedback;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FeedbackRepositoryR2dbc extends ReactiveCrudRepository<FeedbackEntity, UUID> {
  Flux<FeedbackEntity> findAllByUserIdAndPizzeriaIdOrderByCreatedAtDesc(
      UUID userId, UUID pizzeriaId);

  Flux<FeedbackEntity> findAllByPizzeriaIdOrderByCreatedAtDesc(UUID pizzeriaId);

  Mono<FeedbackEntity> findByIdAndPizzeriaId(UUID id, UUID pizzeriaId);

  @Query(
      "SELECT COUNT(*) FROM feedback "
          + "WHERE user_id = :userId AND pizzeria_id = :pizzeriaId "
          + "AND admin_reply IS NOT NULL AND admin_reply_read_at IS NULL")
  Mono<Long> countUnreadRepliesByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId);

  @Modifying
  @Query(
      "UPDATE feedback SET admin_reply_read_at = :readAt "
          + "WHERE user_id = :userId AND pizzeria_id = :pizzeriaId "
          + "AND admin_reply IS NOT NULL AND admin_reply_read_at IS NULL")
  Mono<Long> markAllRepliesAsReadByUserIdAndPizzeriaId(
      UUID userId, UUID pizzeriaId, Instant readAt);
}
