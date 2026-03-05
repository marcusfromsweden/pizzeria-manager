package com.pizzeriaservice.service.order;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface OrderItemRepositoryR2dbc extends ReactiveCrudRepository<OrderItemEntity, UUID> {

  Flux<OrderItemEntity> findAllByOrderIdOrderByCreatedAt(UUID orderId);

  Flux<OrderItemEntity> findAllByOrderIdIn(Collection<UUID> orderIds);
}
