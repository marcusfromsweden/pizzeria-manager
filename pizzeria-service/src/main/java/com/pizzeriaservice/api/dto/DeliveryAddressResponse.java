package com.pizzeriaservice.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAddressResponse(
    UUID id,
    String label,
    String street,
    String postalCode,
    String city,
    String phone,
    String instructions,
    boolean isDefault,
    Instant createdAt) {}
