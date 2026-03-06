package com.pizzeriaservice.service.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.pizzeriaservice.api.dto.MenuIngredientResponse;
import com.pizzeriaservice.api.dto.MenuItemResponse;
import com.pizzeriaservice.api.dto.MenuResponse;
import com.pizzeriaservice.service.test.PizzeriaIntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

@PizzeriaIntegrationTest
class MenuCalorieIntegrationTest {

  @Autowired private WebTestClient webTestClient;

  @Test
  void getMenuShouldReturnMenuItemsWithCalories() {
    webTestClient
        .get()
        .uri("/api/v1/pizzerias/ramonamalmo/menu")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(MenuResponse.class)
        .value(
            response -> {
              assertThat(response.sections()).isNotEmpty();

              // Find a pizza section
              var pizzaSection =
                  response.sections().stream()
                      .filter(section -> section.code().contains("pizza"))
                      .findFirst();

              assertThat(pizzaSection).isPresent();
              assertThat(pizzaSection.get().items()).isNotEmpty();

              // Verify that at least one item has calorie data
              MenuItemResponse item = pizzaSection.get().items().get(0);
              assertThat(item.totalCalories()).isNotNull();
              assertThat(item.totalCalories()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            });
  }

  @Test
  void getMenuShouldReturnIngredientsWithCalories() {
    webTestClient
        .get()
        .uri("/api/v1/pizzerias/ramonamalmo/menu")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(MenuResponse.class)
        .value(
            response -> {
              assertThat(response.sections()).isNotEmpty();

              // Find an item with ingredients
              var itemWithIngredients =
                  response.sections().stream()
                      .flatMap(section -> section.items().stream())
                      .filter(item -> !item.ingredients().isEmpty())
                      .findFirst();

              if (itemWithIngredients.isPresent()) {
                MenuItemResponse item = itemWithIngredients.get();
                assertThat(item.ingredients()).isNotEmpty();

                // Verify ingredients have calorie data
                for (MenuIngredientResponse ingredient : item.ingredients()) {
                  assertThat(ingredient.caloriesPer100g()).isNotNull();
                  assertThat(ingredient.caloriesPer100g()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
                }
              }
            });
  }

  @Test
  void getMenuShouldHaveReasonableCalorieValues() {
    webTestClient
        .get()
        .uri("/api/v1/pizzerias/ramonamalmo/menu")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(MenuResponse.class)
        .value(
            response -> {
              // Find pizza items and verify calorie values are reasonable
              var pizzaItems =
                  response.sections().stream()
                      .filter(section -> section.code().contains("pizza"))
                      .flatMap(section -> section.items().stream())
                      .toList();

              assertThat(pizzaItems).isNotEmpty();

              for (MenuItemResponse item : pizzaItems) {
                // Pizza calories should typically be between 0 and 3000
                assertThat(item.totalCalories())
                    .isGreaterThanOrEqualTo(BigDecimal.ZERO)
                    .isLessThanOrEqualTo(BigDecimal.valueOf(3000));

                // If item has ingredients, verify their calorie values
                for (MenuIngredientResponse ingredient : item.ingredients()) {
                  // Ingredient calories per 100g should be between 0 and 900
                  assertThat(ingredient.caloriesPer100g())
                      .isGreaterThanOrEqualTo(BigDecimal.ZERO)
                      .isLessThanOrEqualTo(BigDecimal.valueOf(900));
                }
              }
            });
  }
}
