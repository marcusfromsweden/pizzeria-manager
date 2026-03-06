package com.pizzeriaservice.service.menu;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MenuIngredientFactEntityTest {

  @Test
  void shouldCreateIngredientFactWithCalories() {
    UUID id = UUID.randomUUID();
    UUID pizzeriaId = UUID.randomUUID();
    BigDecimal caloriesPer100g = BigDecimal.valueOf(280.00);

    MenuIngredientFactEntity entity =
        new MenuIngredientFactEntity(
            id,
            pizzeriaId,
            "translation.key.ingredient.mozzarella",
            "NONE",
            "DAIRY",
            0,
            caloriesPer100g);

    assertThat(entity.id()).isEqualTo(id);
    assertThat(entity.caloriesPer100g()).isEqualTo(caloriesPer100g);
    assertThat(entity.ingredientKey()).isEqualTo("translation.key.ingredient.mozzarella");
  }

  @Test
  void shouldHandleLowCalorieIngredient() {
    UUID id = UUID.randomUUID();
    UUID pizzeriaId = UUID.randomUUID();
    BigDecimal lowCalories = BigDecimal.valueOf(18.00);

    MenuIngredientFactEntity entity =
        new MenuIngredientFactEntity(
            id, pizzeriaId, "translation.key.ingredient.tomatoes", "VEGAN", "", 0, lowCalories);

    assertThat(entity.caloriesPer100g()).isEqualTo(lowCalories);
    assertThat(entity.caloriesPer100g()).isGreaterThan(BigDecimal.ZERO);
    assertThat(entity.dietaryType()).isEqualTo("VEGAN");
  }

  @Test
  void shouldHandleHighCalorieIngredient() {
    UUID id = UUID.randomUUID();
    UUID pizzeriaId = UUID.randomUUID();
    BigDecimal highCalories = BigDecimal.valueOf(884.00);

    MenuIngredientFactEntity entity =
        new MenuIngredientFactEntity(
            id, pizzeriaId, "translation.key.ingredient.olive.oil", "VEGAN", "", 0, highCalories);

    assertThat(entity.caloriesPer100g()).isEqualTo(highCalories);
    assertThat(entity.caloriesPer100g()).isGreaterThan(BigDecimal.valueOf(500));
  }
}
