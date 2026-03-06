package com.pizzeriaservice.api.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record MenuIngredientResponse(
    UUID id,
    String ingredientKey,
    String dietaryType,
    Set<String> allergenTags,
    int spiceLevel,
    BigDecimal caloriesPer100g) {}
