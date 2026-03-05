package com.pizzeriaservice.service.domain.model;

import com.pizzeriaservice.service.domain.FulfillmentType;
import com.pizzeriaservice.service.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Order(
    UUID id,
    UUID pizzeriaId,
    UUID userId,
    String orderNumber,
    OrderStatus status,
    FulfillmentType fulfillmentType,
    UUID deliveryAddressId,
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
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy,
    List<OrderItem> items,
    boolean isNew) {}
