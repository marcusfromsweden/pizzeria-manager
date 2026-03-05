package com.pizzeriaservice.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MenuItemResponse(
    UUID id,
    UUID sectionId,
    int dishNumber,
    String nameKey,
    String descriptionKey,
    BigDecimal priceInSek,
    BigDecimal familySizePriceInSek,
    List<MenuIngredientResponse> ingredients,
    String overallDietaryType,
    int sortOrder) {}
