package com.pizzeriaservice.service.order;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderRepositoryR2dbc extends ReactiveCrudRepository<OrderEntity, UUID> {

  Flux<OrderEntity> findAllByUserIdAndPizzeriaIdOrderByCreatedAtDesc(UUID userId, UUID pizzeriaId);

  @Query(
      "SELECT * FROM orders WHERE user_id = :userId AND pizzeria_id = :pizzeriaId "
          + "AND status NOT IN ('DELIVERED', 'PICKED_UP', 'CANCELLED') "
          + "ORDER BY created_at DESC")
  Flux<OrderEntity> findActiveByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId);

  Mono<OrderEntity> findByIdAndUserIdAndPizzeriaId(UUID id, UUID userId, UUID pizzeriaId);

  Mono<OrderEntity> findByIdAndPizzeriaId(UUID id, UUID pizzeriaId);

  @Query(
      "SELECT COALESCE(MAX(CAST(SUBSTRING(order_number FROM '[0-9]+$') AS INTEGER)), 0) "
          + "FROM orders WHERE pizzeria_id = :pizzeriaId "
          + "AND order_number LIKE :prefix || '%'")
  Mono<Integer> findMaxOrderNumberForPrefix(UUID pizzeriaId, String prefix);
}
