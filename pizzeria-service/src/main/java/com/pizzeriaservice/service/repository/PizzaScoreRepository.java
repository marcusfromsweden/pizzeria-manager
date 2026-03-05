package com.pizzeriaservice.service.repository;

import com.pizzeriaservice.service.domain.model.PizzaScore;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PizzaScoreRepository {
  Mono<PizzaScore> save(PizzaScore score);

  Flux<PizzaScore> findByUserId(UUID userId);

  Flux<PizzaScore> findByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId);
}
