package com.pizzeriaservice.service.repository.r2dbc;

import com.pizzeriaservice.service.domain.model.DeliveryAddress;
import com.pizzeriaservice.service.order.DeliveryAddressEntity;
import com.pizzeriaservice.service.order.DeliveryAddressRepositoryR2dbc;
import com.pizzeriaservice.service.repository.DeliveryAddressRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class DeliveryAddressRepositoryAdapter implements DeliveryAddressRepository {

  private final DeliveryAddressRepositoryR2dbc repository;

  @Override
  public Mono<DeliveryAddress> save(DeliveryAddress address) {
    DeliveryAddressEntity entity = toEntity(address);
    return repository.save(entity).map(DeliveryAddressRepositoryAdapter::toDomain);
  }

  @Override
  public Flux<DeliveryAddress> findByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId) {
    return repository
        .findAllByUserIdAndPizzeriaIdOrderByIsDefaultDescCreatedAtDesc(userId, pizzeriaId)
        .map(DeliveryAddressRepositoryAdapter::toDomain);
  }

  @Override
  public Mono<DeliveryAddress> findByIdAndUserIdAndPizzeriaId(
      UUID id, UUID userId, UUID pizzeriaId) {
    return repository
        .findByIdAndUserIdAndPizzeriaId(id, userId, pizzeriaId)
        .map(DeliveryAddressRepositoryAdapter::toDomain);
  }

  @Override
  public Mono<Void> deleteByIdAndUserIdAndPizzeriaId(UUID id, UUID userId, UUID pizzeriaId) {
    return repository.deleteByIdAndUserIdAndPizzeriaId(id, userId, pizzeriaId);
  }

  @Override
  public Mono<Void> clearDefaultByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId) {
    return repository.clearDefaultByUserIdAndPizzeriaId(userId, pizzeriaId).then();
  }

  @Override
  public Mono<Long> countByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId) {
    return repository.countByUserIdAndPizzeriaId(userId, pizzeriaId);
  }

  private static DeliveryAddressEntity toEntity(DeliveryAddress address) {
    if (address.isNew()) {
      return DeliveryAddressEntity.builder()
          .id(address.id())
          .pizzeriaId(address.pizzeriaId())
          .userId(address.userId())
          .label(address.label())
          .street(address.street())
          .postalCode(address.postalCode())
          .city(address.city())
          .phone(address.phone())
          .instructions(address.instructions())
          .isDefault(address.isDefault())
          .createdAt(address.createdAt())
          .updatedAt(address.updatedAt())
          .isNew(true)
          .build();
    }
    return DeliveryAddressEntity.builder()
        .id(address.id())
        .pizzeriaId(address.pizzeriaId())
        .userId(address.userId())
        .label(address.label())
        .street(address.street())
        .postalCode(address.postalCode())
        .city(address.city())
        .phone(address.phone())
        .instructions(address.instructions())
        .isDefault(address.isDefault())
        .createdAt(address.createdAt())
        .updatedAt(address.updatedAt())
        .build();
  }

  private static DeliveryAddress toDomain(DeliveryAddressEntity entity) {
    return DeliveryAddress.builder()
        .id(entity.getId())
        .pizzeriaId(entity.getPizzeriaId())
        .userId(entity.getUserId())
        .label(entity.getLabel())
        .street(entity.getStreet())
        .postalCode(entity.getPostalCode())
        .city(entity.getCity())
        .phone(entity.getPhone())
        .instructions(entity.getInstructions())
        .isDefault(entity.isDefault())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .isNew(false)
        .build();
  }
}
