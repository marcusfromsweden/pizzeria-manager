package com.pizzeriaservice.service.domain.model;

import com.pizzeriaservice.service.domain.PizzaKind;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record PizzaScore(
    UUID id,
    UUID pizzeriaId,
    UUID userId,
    UUID pizzaId,
    PizzaKind pizzaKind,
    int score,
    String comment,
    Instant createdAt,
    UUID createdBy,
    boolean isNew) {}
