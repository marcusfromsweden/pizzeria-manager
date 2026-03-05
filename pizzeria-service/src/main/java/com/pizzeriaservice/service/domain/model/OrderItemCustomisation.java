package com.pizzeriaservice.service.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record OrderItemCustomisation(
    UUID id,
    UUID pizzeriaId,
    UUID orderItemId,
    UUID customisationId,
    String customisationNameKey,
    BigDecimal price,
    Instant createdAt,
    boolean isNew) {}
