package com.pizzeriaservice.service.repository;

import com.pizzeriaservice.service.domain.model.Order;
import com.pizzeriaservice.service.domain.model.OrderItem;
import com.pizzeriaservice.service.domain.model.OrderItemCustomisation;
import java.util.Collection;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderRepository {

  Mono<Order> save(Order order);

  Mono<OrderItem> saveOrderItem(OrderItem item);

  Mono<OrderItemCustomisation> saveOrderItemCustomisation(OrderItemCustomisation customisation);

  Flux<Order> findByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId);

  Flux<Order> findActiveByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId);

  Mono<Order> findByIdAndUserIdAndPizzeriaId(UUID id, UUID userId, UUID pizzeriaId);

  Mono<Order> findByIdAndPizzeriaId(UUID id, UUID pizzeriaId);

  Flux<OrderItem> findItemsByOrderId(UUID orderId);

  Flux<OrderItem> findItemsByOrderIds(Collection<UUID> orderIds);

  Flux<OrderItemCustomisation> findCustomisationsByOrderItemId(UUID orderItemId);

  Flux<OrderItemCustomisation> findCustomisationsByOrderItemIds(Collection<UUID> orderItemIds);

  Mono<Integer> findMaxOrderNumberForPrefix(UUID pizzeriaId, String prefix);
}
