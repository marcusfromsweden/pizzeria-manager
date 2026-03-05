package com.pizzeriaservice.service.repository;

import com.pizzeriaservice.service.domain.model.DeliveryAddress;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeliveryAddressRepository {

  Mono<DeliveryAddress> save(DeliveryAddress address);

  Flux<DeliveryAddress> findByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId);

  Mono<DeliveryAddress> findByIdAndUserIdAndPizzeriaId(UUID id, UUID userId, UUID pizzeriaId);

  Mono<Void> deleteByIdAndUserIdAndPizzeriaId(UUID id, UUID userId, UUID pizzeriaId);

  Mono<Void> clearDefaultByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId);

  Mono<Long> countByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId);
}
