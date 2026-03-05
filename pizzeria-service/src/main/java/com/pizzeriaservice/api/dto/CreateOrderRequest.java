package com.pizzeriaservice.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
    @NotNull String fulfillmentType,
    UUID deliveryAddressId,
    String deliveryStreet,
    String deliveryPostalCode,
    String deliveryCity,
    String deliveryPhone,
    String deliveryInstructions,
    Instant requestedTime,
    String customerNotes,
    @NotEmpty @Valid List<OrderItemRequest> items) {}
