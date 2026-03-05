package com.pizzeriaservice.service.repository.inmemory;

import com.pizzeriaservice.service.domain.model.Feedback;
import com.pizzeriaservice.service.repository.FeedbackRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class InMemoryFeedbackRepository implements FeedbackRepository {

  private final Map<UUID, Feedback> storage = new ConcurrentHashMap<>();

  @Override
  public Mono<Feedback> save(Feedback feedback) {
    if (feedback == null || feedback.id() == null) {
      return Mono.error(new IllegalArgumentException("Feedback and feedback.id must be non-null"));
    }
    storage.put(feedback.id(), feedback);
    return Mono.just(feedback);
  }

  @Override
  public Flux<Feedback> findByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId) {
    return Flux.fromIterable(storage.values())
        .filter(f -> f.userId().equals(userId) && f.pizzeriaId().equals(pizzeriaId))
        .sort(Comparator.comparing(Feedback::createdAt).reversed());
  }

  @Override
  public Flux<Feedback> findByPizzeriaId(UUID pizzeriaId) {
    return Flux.fromIterable(storage.values())
        .filter(f -> f.pizzeriaId().equals(pizzeriaId))
        .sort(Comparator.comparing(Feedback::createdAt).reversed());
  }

  @Override
  public Mono<Feedback> findByIdAndPizzeriaId(UUID id, UUID pizzeriaId) {
    return Mono.justOrEmpty(storage.get(id)).filter(f -> f.pizzeriaId().equals(pizzeriaId));
  }

  @Override
  public Mono<Long> countUnreadRepliesByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId) {
    long count =
        storage.values().stream()
            .filter(
                f ->
                    f.userId().equals(userId)
                        && f.pizzeriaId().equals(pizzeriaId)
                        && f.adminReply() != null
                        && f.adminReplyReadAt() == null)
            .count();
    return Mono.just(count);
  }

  @Override
  public Mono<Void> markAllRepliesAsReadByUserIdAndPizzeriaId(
      UUID userId, UUID pizzeriaId, Instant readAt) {
    storage.replaceAll(
        (id, feedback) -> {
          if (feedback.userId().equals(userId)
              && feedback.pizzeriaId().equals(pizzeriaId)
              && feedback.adminReply() != null
              && feedback.adminReplyReadAt() == null) {
            return feedback.toBuilder().adminReplyReadAt(readAt).build();
          }
          return feedback;
        });
    return Mono.empty();
  }

  public void clear() {
    storage.clear();
  }

  public Feedback findById(UUID id) {
    return storage.get(id);
  }

  public Map<UUID, Feedback> findAll() {
    return Map.copyOf(storage);
  }
}
