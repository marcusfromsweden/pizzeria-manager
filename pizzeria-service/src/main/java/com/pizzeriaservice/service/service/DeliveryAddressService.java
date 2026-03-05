package com.pizzeriaservice.service.service;

import com.pizzeriaservice.api.dto.DeliveryAddressResponse;
import com.pizzeriaservice.api.dto.SaveDeliveryAddressRequest;
import com.pizzeriaservice.service.converter.RestDomainConverter;
import com.pizzeriaservice.service.domain.model.DeliveryAddress;
import com.pizzeriaservice.service.repository.DeliveryAddressRepository;
import com.pizzeriaservice.service.support.TimeProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeliveryAddressService {

  private static final int MAX_ADDRESSES_PER_USER = 10;

  private final DeliveryAddressRepository addressRepository;
  private final TimeProvider timeProvider;
  private final RestDomainConverter converter;

  public Flux<DeliveryAddressResponse> getAddresses(UUID userId, UUID pizzeriaId) {
    return addressRepository
        .findByUserIdAndPizzeriaId(userId, pizzeriaId)
        .map(converter::toDeliveryAddressResponse);
  }

  public Mono<DeliveryAddressResponse> saveAddress(
      UUID userId, UUID pizzeriaId, SaveDeliveryAddressRequest request) {
    return addressRepository
        .countByUserIdAndPizzeriaId(userId, pizzeriaId)
        .flatMap(
            count -> {
              if (count >= MAX_ADDRESSES_PER_USER) {
                return Mono.error(new IllegalStateException("Maximum number of addresses reached"));
              }
              return createAddress(userId, pizzeriaId, request);
            });
  }

  private Mono<DeliveryAddressResponse> createAddress(
      UUID userId, UUID pizzeriaId, SaveDeliveryAddressRequest request) {
    Mono<Void> clearDefault =
        request.isDefault()
            ? addressRepository.clearDefaultByUserIdAndPizzeriaId(userId, pizzeriaId)
            : Mono.empty();

    return clearDefault.then(
        Mono.defer(
            () -> {
              DeliveryAddress address =
                  DeliveryAddress.builder()
                      .id(UUID.randomUUID())
                      .pizzeriaId(pizzeriaId)
                      .userId(userId)
                      .label(request.label())
                      .street(request.street())
                      .postalCode(request.postalCode())
                      .city(request.city())
                      .phone(request.phone())
                      .instructions(request.instructions())
                      .isDefault(request.isDefault())
                      .createdAt(timeProvider.now())
                      .updatedAt(timeProvider.now())
                      .isNew(true)
                      .build();
              return addressRepository.save(address).map(converter::toDeliveryAddressResponse);
            }));
  }

  public Mono<Void> deleteAddress(UUID addressId, UUID userId, UUID pizzeriaId) {
    return addressRepository.deleteByIdAndUserIdAndPizzeriaId(addressId, userId, pizzeriaId);
  }

  public Mono<DeliveryAddressResponse> setDefaultAddress(
      UUID addressId, UUID userId, UUID pizzeriaId) {
    return addressRepository
        .findByIdAndUserIdAndPizzeriaId(addressId, userId, pizzeriaId)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Address not found")))
        .flatMap(
            address ->
                addressRepository
                    .clearDefaultByUserIdAndPizzeriaId(userId, pizzeriaId)
                    .then(
                        Mono.defer(
                            () -> {
                              DeliveryAddress updated =
                                  address.toBuilder()
                                      .isDefault(true)
                                      .updatedAt(timeProvider.now())
                                      .isNew(false)
                                      .build();
                              return addressRepository.save(updated);
                            })))
        .map(converter::toDeliveryAddressResponse);
  }
}
