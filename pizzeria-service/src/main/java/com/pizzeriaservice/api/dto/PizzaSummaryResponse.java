package com.pizzeriaservice.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PizzaSummaryResponse(
    UUID id,
    int dishNumber,
    String nameKey,
    BigDecimal priceInSek,
    BigDecimal familySizePriceInSek,
    String overallDietaryType,
    int sortOrder,
    BigDecimal totalCalories) {}
