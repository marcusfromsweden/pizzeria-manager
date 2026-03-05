package com.pizzeriaservice.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PreferredIngredientRequest(@NotNull UUID ingredientId) {}
