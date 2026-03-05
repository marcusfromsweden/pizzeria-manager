package com.pizzeriaservice.service.menu;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PizzaCustomisationRepository
    extends ReactiveCrudRepository<PizzaCustomisationEntity, UUID> {
  Flux<PizzaCustomisationEntity> findAllByPizzeriaIdOrderBySortOrderAsc(UUID pizzeriaId);
}
