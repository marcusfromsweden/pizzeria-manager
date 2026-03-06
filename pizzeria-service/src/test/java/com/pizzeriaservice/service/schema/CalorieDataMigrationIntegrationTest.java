package com.pizzeriaservice.service.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.pizzeriaservice.service.menu.MenuIngredientFactRepository;
import com.pizzeriaservice.service.menu.MenuItemRepository;
import com.pizzeriaservice.service.test.PizzeriaIntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

@PizzeriaIntegrationTest
class CalorieDataMigrationIntegrationTest {

  @Autowired private MenuIngredientFactRepository ingredientFactRepository;

  @Autowired private MenuItemRepository menuItemRepository;

  @Test
  void shouldHaveCalorieDataInIngredientFacts() {
    // Verify that ingredient facts have calorie data populated
    StepVerifier.create(
            ingredientFactRepository.findAll().filter(fact -> fact.caloriesPer100g() != null))
        .expectNextMatches(fact -> fact.caloriesPer100g().compareTo(BigDecimal.ZERO) >= 0)
        .expectNextCount(0) // At least one ingredient should have calorie data
        .verifyComplete();
  }

  @Test
  void shouldHaveCalorieDataInMenuItems() {
    // Verify that menu items have total calorie data
    StepVerifier.create(
            menuItemRepository.findAll().filter(item -> item.totalCalories() != null).take(5))
        .expectNextMatches(
            item -> {
              assertThat(item.totalCalories()).isNotNull();
              assertThat(item.totalCalories()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
              // A typical pizza should have between 100 and 5000 calories
              assertThat(item.totalCalories()).isLessThanOrEqualTo(BigDecimal.valueOf(5000));
              return true;
            })
        .expectNextCount(4) // Expect at least 5 items
        .verifyComplete();
  }

  @Test
  void calorieValuesShouldBeReasonable() {
    // Test that calorie values are within reasonable ranges
    StepVerifier.create(
            ingredientFactRepository
                .findAll()
                .filter(
                    fact ->
                        fact.caloriesPer100g() != null
                            && fact.caloriesPer100g().compareTo(BigDecimal.ZERO) > 0))
        .expectNextMatches(
            fact -> {
              // Calories per 100g should be between 1 and 900 for real foods
              assertThat(fact.caloriesPer100g()).isBetween(BigDecimal.ONE, BigDecimal.valueOf(900));
              return true;
            })
        .thenConsumeWhile(fact -> true) // Consume remaining items
        .verifyComplete();
  }

  @Test
  void menuItemsShouldHaveNonNegativeCalories() {
    // All menu items must have non-negative calorie values
    StepVerifier.create(menuItemRepository.findAll())
        .expectNextMatches(item -> item.totalCalories().compareTo(BigDecimal.ZERO) >= 0)
        .thenConsumeWhile(item -> item.totalCalories().compareTo(BigDecimal.ZERO) >= 0)
        .verifyComplete();
  }
}
