package com.pizzeriaservice.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemCustomisationResponse(
    UUID id, UUID customisationId, String customisationNameKey, BigDecimal price) {}
