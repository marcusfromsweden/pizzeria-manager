package com.pizzeriaservice.service.user;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserPreferredIngredientRepository
    extends ReactiveCrudRepository<UserPreferredIngredientEntity, UUID> {
  Flux<UserPreferredIngredientEntity> findAllByUserId(UUID userId);

  Mono<Long> deleteByUserId(UUID userId);

  Mono<Long> deleteByUserIdAndIngredientId(UUID userId, UUID ingredientId);
}
