package com.pizzeriaservice.service.pizzascore;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PizzaScoreRepositoryR2dbc extends ReactiveCrudRepository<PizzaScoreEntity, UUID> {
  Flux<PizzaScoreEntity> findAllByUserId(UUID userId);

  Flux<PizzaScoreEntity> findAllByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId);
}
