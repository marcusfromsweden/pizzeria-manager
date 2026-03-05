package com.pizzeriaservice.service.menu;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MenuItemRepository extends ReactiveCrudRepository<MenuItemEntity, UUID> {
  Flux<MenuItemEntity> findAllBySectionIdOrderBySortOrderAsc(UUID sectionId);

  Flux<MenuItemEntity> findAllByOrderBySectionIdAscSortOrderAsc();

  Mono<MenuItemEntity> findByIdAndPizzeriaId(UUID id, UUID pizzeriaId);

  Flux<MenuItemEntity> findAllByPizzeriaIdOrderBySortOrderAsc(UUID pizzeriaId);
}
