package com.pizzeriaservice.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    String orderNumber,
    String status,
    String fulfillmentType,
    String deliveryStreet,
    String deliveryPostalCode,
    String deliveryCity,
    String deliveryPhone,
    String deliveryInstructions,
    Instant requestedTime,
    Instant estimatedReadyTime,
    BigDecimal subtotal,
    BigDecimal deliveryFee,
    BigDecimal total,
    String customerNotes,
    List<OrderItemResponse> items,
    Instant createdAt,
    Instant updatedAt) {}
