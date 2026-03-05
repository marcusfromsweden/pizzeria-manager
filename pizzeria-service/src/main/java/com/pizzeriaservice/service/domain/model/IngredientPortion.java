package com.pizzeriaservice.service.domain.model;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record IngredientPortion(UUID ingredientId, double quantity, BigDecimal extraCost) {
  public IngredientPortion {
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
  }
}
