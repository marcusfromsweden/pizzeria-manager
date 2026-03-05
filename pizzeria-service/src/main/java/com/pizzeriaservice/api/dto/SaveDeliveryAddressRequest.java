package com.pizzeriaservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveDeliveryAddressRequest(
    @Size(max = 50) String label,
    @NotBlank @Size(max = 255) String street,
    @NotBlank @Size(max = 20) String postalCode,
    @NotBlank @Size(max = 100) String city,
    @Size(max = 30) String phone,
    String instructions,
    boolean isDefault) {}
