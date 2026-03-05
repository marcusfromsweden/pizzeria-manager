package com.pizzeriaservice.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderItemResponse(
    UUID id,
    UUID menuItemId,
    String menuItemNameKey,
    String size,
    int quantity,
    BigDecimal basePrice,
    BigDecimal customisationsPrice,
    BigDecimal itemTotal,
    String specialInstructions,
    List<OrderItemCustomisationResponse> customisations) {}
