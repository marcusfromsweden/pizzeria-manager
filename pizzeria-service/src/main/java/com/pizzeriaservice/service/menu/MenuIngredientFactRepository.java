package com.pizzeriaservice.service.menu;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MenuIngredientFactRepository
    extends ReactiveCrudRepository<MenuIngredientFactEntity, UUID> {
  Mono<MenuIngredientFactEntity> findByIngredientKeyAndPizzeriaId(
      String ingredientKey, UUID pizzeriaId);

  Mono<MenuIngredientFactEntity> findByIdAndPizzeriaId(UUID id, UUID pizzeriaId);

  Flux<MenuIngredientFactEntity> findAllByIdInAndPizzeriaId(Collection<UUID> ids, UUID pizzeriaId);

  Flux<MenuIngredientFactEntity> findAllByPizzeriaId(UUID pizzeriaId);
}
