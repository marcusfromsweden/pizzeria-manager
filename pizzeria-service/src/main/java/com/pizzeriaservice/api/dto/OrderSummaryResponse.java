package com.pizzeriaservice.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
    UUID id,
    String orderNumber,
    String status,
    String fulfillmentType,
    BigDecimal total,
    int itemCount,
    Instant createdAt) {}
