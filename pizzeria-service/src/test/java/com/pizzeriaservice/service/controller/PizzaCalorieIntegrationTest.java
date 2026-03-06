package com.pizzeriaservice.service.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.pizzeriaservice.api.dto.MenuIngredientResponse;
import com.pizzeriaservice.api.dto.PizzaDetailResponse;
import com.pizzeriaservice.api.dto.PizzaSummaryResponse;
import com.pizzeriaservice.service.test.PizzeriaIntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

@PizzeriaIntegrationTest
class PizzaCalorieIntegrationTest {

  @Autowired private WebTestClient webTestClient;

  @Test
  void listPizzasShouldReturnCalories() {
    webTestClient
        .get()
        .uri("/api/v1/pizzerias/ramonamalmo/pizzas")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(PizzaSummaryResponse.class)
        .value(
            pizzas -> {
              assertThat(pizzas).isNotEmpty();

              // Verify each pizza has calorie data
              for (PizzaSummaryResponse pizza : pizzas) {
                assertThat(pizza.totalCalories()).isNotNull();
                assertThat(pizza.totalCalories()).isGreaterThanOrEqualTo(BigDecimal.ZERO);

                // Pizza calories should be reasonable (0-3000)
                assertThat(pizza.totalCalories()).isLessThanOrEqualTo(BigDecimal.valueOf(3000));
              }
            });
  }

  @Test
  void listPizzasShouldHaveVariedCalories() {
    webTestClient
        .get()
        .uri("/api/v1/pizzerias/ramonamalmo/pizzas")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(PizzaSummaryResponse.class)
        .value(
            pizzas -> {
              assertThat(pizzas).hasSizeGreaterThanOrEqualTo(2);

              // Extract unique calorie values (pizzas should have different calories)
              List<BigDecimal> calorieValues =
                  pizzas.stream().map(PizzaSummaryResponse::totalCalories).distinct().toList();

              // We expect some variation in calorie values across pizzas
              // (not all pizzas should have exactly the same calories)
              assertThat(calorieValues.size()).isGreaterThan(1);
            });
  }

  @Test
  void getPizzaByIdShouldReturnCalories() {
    // First, get the list of pizzas to find a valid ID
    List<PizzaSummaryResponse> pizzas =
        webTestClient
            .get()
            .uri("/api/v1/pizzerias/ramonamalmo/pizzas")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(PizzaSummaryResponse.class)
            .returnResult()
            .getResponseBody();

    assertThat(pizzas).isNotEmpty();
    UUID pizzaId = pizzas.get(0).id();

    // Now get the pizza details
    webTestClient
        .get()
        .uri("/api/v1/pizzerias/ramonamalmo/pizzas/{pizzaId}", pizzaId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(PizzaDetailResponse.class)
        .value(
            pizza -> {
              assertThat(pizza.id()).isEqualTo(pizzaId);
              assertThat(pizza.totalCalories()).isNotNull();
              assertThat(pizza.totalCalories()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
              assertThat(pizza.totalCalories()).isLessThanOrEqualTo(BigDecimal.valueOf(3000));
            });
  }

  @Test
  void getPizzaByIdShouldReturnIngredientCalories() {
    // First, get the list of pizzas to find a valid ID
    List<PizzaSummaryResponse> pizzas =
        webTestClient
            .get()
            .uri("/api/v1/pizzerias/ramonamalmo/pizzas")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(PizzaSummaryResponse.class)
            .returnResult()
            .getResponseBody();

    assertThat(pizzas).isNotEmpty();
    UUID pizzaId = pizzas.get(0).id();

    // Now get the pizza details and verify ingredient calories
    webTestClient
        .get()
        .uri("/api/v1/pizzerias/ramonamalmo/pizzas/{pizzaId}", pizzaId)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(PizzaDetailResponse.class)
        .value(
            pizza -> {
              assertThat(pizza.ingredients()).isNotEmpty();

              // Verify each ingredient has calorie data
              for (MenuIngredientResponse ingredient : pizza.ingredients()) {
                assertThat(ingredient.caloriesPer100g()).isNotNull();
                assertThat(ingredient.caloriesPer100g()).isGreaterThanOrEqualTo(BigDecimal.ZERO);

                // Ingredient calories per 100g should be reasonable (0-900)
                assertThat(ingredient.caloriesPer100g())
                    .isLessThanOrEqualTo(BigDecimal.valueOf(900));
              }
            });
  }

  @Test
  void comparePizzaCaloriesAcrossEndpoints() {
    // Get a pizza from the list endpoint
    List<PizzaSummaryResponse> pizzas =
        webTestClient
            .get()
            .uri("/api/v1/pizzerias/ramonamalmo/pizzas")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(PizzaSummaryResponse.class)
            .returnResult()
            .getResponseBody();

    assertThat(pizzas).isNotEmpty();
    PizzaSummaryResponse pizzaSummary = pizzas.get(0);

    // Get the same pizza from the detail endpoint
    PizzaDetailResponse pizzaDetail =
        webTestClient
            .get()
            .uri("/api/v1/pizzerias/ramonamalmo/pizzas/{pizzaId}", pizzaSummary.id())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(PizzaDetailResponse.class)
            .returnResult()
            .getResponseBody();

    // Verify that calories match across both endpoints
    assertThat(pizzaDetail).isNotNull();
    assertThat(pizzaDetail.totalCalories()).isEqualTo(pizzaSummary.totalCalories());
  }
}
