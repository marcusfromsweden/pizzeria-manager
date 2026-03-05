package com.pizzeriaservice.service.order;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface OrderItemCustomisationRepositoryR2dbc
    extends ReactiveCrudRepository<OrderItemCustomisationEntity, UUID> {

  Flux<OrderItemCustomisationEntity> findAllByOrderItemIdOrderByCreatedAt(UUID orderItemId);

  Flux<OrderItemCustomisationEntity> findAllByOrderItemIdIn(Collection<UUID> orderItemIds);
}
