package com.pizzeriaservice.service.domain.model;

import com.pizzeriaservice.service.domain.PizzaSize;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record OrderItem(
    UUID id,
    UUID pizzeriaId,
    UUID orderId,
    UUID menuItemId,
    String menuItemNameKey,
    PizzaSize size,
    int quantity,
    BigDecimal basePrice,
    BigDecimal customisationsPrice,
    BigDecimal itemTotal,
    String specialInstructions,
    Instant createdAt,
    UUID createdBy,
    List<OrderItemCustomisation> customisations,
    boolean isNew) {}
