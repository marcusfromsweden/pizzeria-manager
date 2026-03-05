package com.pizzeriaservice.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record PizzaPricePreviewRequest(
    @NotNull UUID pizzaId, List<UUID> additionalIngredientIds, List<UUID> removedIngredientIds) {}
