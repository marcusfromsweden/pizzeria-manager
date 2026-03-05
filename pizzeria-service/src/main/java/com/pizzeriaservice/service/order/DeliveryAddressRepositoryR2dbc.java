package com.pizzeriaservice.service.order;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeliveryAddressRepositoryR2dbc
    extends ReactiveCrudRepository<DeliveryAddressEntity, UUID> {

  Flux<DeliveryAddressEntity> findAllByUserIdAndPizzeriaIdOrderByIsDefaultDescCreatedAtDesc(
      UUID userId, UUID pizzeriaId);

  Mono<DeliveryAddressEntity> findByIdAndUserIdAndPizzeriaId(UUID id, UUID userId, UUID pizzeriaId);

  Mono<Void> deleteByIdAndUserIdAndPizzeriaId(UUID id, UUID userId, UUID pizzeriaId);

  @Modifying
  @Query(
      "UPDATE delivery_addresses SET is_default = false "
          + "WHERE user_id = :userId AND pizzeria_id = :pizzeriaId AND is_default = true")
  Mono<Long> clearDefaultByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId);

  Mono<Long> countByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId);
}
