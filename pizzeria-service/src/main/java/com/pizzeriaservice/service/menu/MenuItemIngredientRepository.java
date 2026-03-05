package com.pizzeriaservice.service.menu;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MenuItemIngredientRepository
    extends ReactiveCrudRepository<MenuItemIngredientEntity, UUID> {
  Flux<MenuItemIngredientEntity> findAllByMenuItemIdOrderBySortOrderAsc(UUID menuItemId);
}
