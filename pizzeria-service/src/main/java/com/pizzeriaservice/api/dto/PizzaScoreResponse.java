package com.pizzeriaservice.api.dto;

import java.time.Instant;
import java.util.UUID;

public record PizzaScoreResponse(
    UUID id,
    UUID userId,
    UUID pizzaId,
    PizzaType pizzaType,
    int score,
    String comment,
    Instant createdAt) {}
