package com.pizzeriaservice.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PizzaDetailResponse(
    UUID id,
    int dishNumber,
    String nameKey,
    String descriptionKey,
    BigDecimal priceInSek,
    BigDecimal familySizePriceInSek,
    List<MenuIngredientResponse> ingredients,
    String overallDietaryType,
    int sortOrder,
    BigDecimal totalCalories) {}
