package com.pizzeriaservice.service.repository;

import com.pizzeriaservice.service.domain.model.Feedback;
import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FeedbackRepository {
  Mono<Feedback> save(Feedback feedback);

  Flux<Feedback> findByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId);

  Flux<Feedback> findByPizzeriaId(UUID pizzeriaId);

  Mono<Feedback> findByIdAndPizzeriaId(UUID id, UUID pizzeriaId);

  Mono<Long> countUnreadRepliesByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId);

  Mono<Void> markAllRepliesAsReadByUserIdAndPizzeriaId(
      UUID userId, UUID pizzeriaId, Instant readAt);
}
