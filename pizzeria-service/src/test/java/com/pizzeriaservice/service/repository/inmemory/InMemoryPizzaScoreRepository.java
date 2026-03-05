package com.pizzeriaservice.service.repository.inmemory;

import com.pizzeriaservice.service.domain.model.PizzaScore;
import com.pizzeriaservice.service.repository.PizzaScoreRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class InMemoryPizzaScoreRepository implements PizzaScoreRepository {

  private final Map<UUID, PizzaScore> storage = new ConcurrentHashMap<>();

  @Override
  public Mono<PizzaScore> save(PizzaScore score) {
    if (score == null || score.id() == null) {
      return Mono.error(new IllegalArgumentException("Score and score.id must be non-null"));
    }
    storage.put(score.id(), score);
    return Mono.just(score);
  }

  @Override
  public Flux<PizzaScore> findByUserId(UUID userId) {
    return Flux.fromIterable(storage.values()).filter(score -> userId.equals(score.userId()));
  }

  @Override
  public Flux<PizzaScore> findByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId) {
    return Flux.fromIterable(storage.values())
        .filter(score -> userId.equals(score.userId()) && pizzeriaId.equals(score.pizzeriaId()));
  }

  public void clear() {
    storage.clear();
  }
}
