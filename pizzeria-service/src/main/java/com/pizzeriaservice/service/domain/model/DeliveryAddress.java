package com.pizzeriaservice.service.domain.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record DeliveryAddress(
    UUID id,
    UUID pizzeriaId,
    UUID userId,
    String label,
    String street,
    String postalCode,
    String city,
    String phone,
    String instructions,
    boolean isDefault,
    Instant createdAt,
    Instant updatedAt,
    boolean isNew) {}
