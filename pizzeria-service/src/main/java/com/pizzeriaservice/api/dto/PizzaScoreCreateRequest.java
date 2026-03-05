package com.pizzeriaservice.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PizzaScoreCreateRequest(
    @NotNull UUID pizzaId,
    @NotNull PizzaType pizzaType,
    @Min(1) @Max(5) int score,
    String comment) {}
