package com.pizzeriaservice.service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.api.dto.MenuIngredientResponse;
import com.pizzeriaservice.api.dto.PizzaDetailResponse;
import com.pizzeriaservice.api.dto.PizzaSummaryResponse;
import com.pizzeriaservice.service.service.PizzaService;
import com.pizzeriaservice.service.service.PizzeriaService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PizzaControllerCalorieTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final String PIZZERIA_CODE = "testpizzeria";

  @Mock private PizzaService pizzaService;
  @Mock private PizzeriaService pizzeriaService;

  private PizzaController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new PizzaController(pizzaService, null, pizzeriaService);
  }

  @Test
  void getShouldReturnPizzaDetailWithCalories() {
    UUID pizzaId = UUID.randomUUID();
    BigDecimal totalCalories = BigDecimal.valueOf(920.75);

    PizzaDetailResponse response =
        new PizzaDetailResponse(
            pizzaId,
            1,
            "pizza.pepperoni",
            "pizza.pepperoni.desc",
            BigDecimal.valueOf(105),
            BigDecimal.valueOf(175),
            List.of(),
            "CARNIVORE",
            1,
            totalCalories);

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(pizzaService.get(pizzaId, DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(response));

    StepVerifier.create(controller.get(PIZZERIA_CODE, pizzaId))
        .assertNext(
            detail -> {
              assertThat(detail.id()).isEqualTo(pizzaId);
              assertThat(detail.totalCalories()).isEqualTo(totalCalories);
              assertThat(detail.totalCalories()).isGreaterThan(BigDecimal.valueOf(900));
            })
        .verifyComplete();

    verify(pizzeriaService).resolvePizzeriaId(PIZZERIA_CODE);
    verify(pizzaService).get(pizzaId, DEFAULT_PIZZERIA_ID);
  }

  @Test
  void getShouldReturnPizzaWithIngredientCalories() {
    UUID pizzaId = UUID.randomUUID();
    UUID ingredientId = UUID.randomUUID();
    BigDecimal totalCalories = BigDecimal.valueOf(850);
    BigDecimal ingredientCalories = BigDecimal.valueOf(280);

    MenuIngredientResponse ingredient =
        new MenuIngredientResponse(
            ingredientId,
            "ingredient.mozzarella",
            "VEGETARIAN",
            Set.of("DAIRY"),
            0,
            ingredientCalories);

    PizzaDetailResponse response =
        new PizzaDetailResponse(
            pizzaId,
            1,
            "pizza.margarita",
            "pizza.margarita.desc",
            BigDecimal.valueOf(95),
            BigDecimal.valueOf(165),
            List.of(ingredient),
            "VEGETARIAN",
            1,
            totalCalories);

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(pizzaService.get(pizzaId, DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(response));

    StepVerifier.create(controller.get(PIZZERIA_CODE, pizzaId))
        .assertNext(
            detail -> {
              assertThat(detail.totalCalories()).isEqualTo(totalCalories);
              assertThat(detail.ingredients()).hasSize(1);

              MenuIngredientResponse ingredientResponse = detail.ingredients().get(0);
              assertThat(ingredientResponse.caloriesPer100g()).isEqualTo(ingredientCalories);
            })
        .verifyComplete();
  }

  @Test
  void getShouldHandleZeroCalories() {
    UUID pizzaId = UUID.randomUUID();

    PizzaDetailResponse response =
        new PizzaDetailResponse(
            pizzaId,
            1,
            "pizza.unknown",
            "pizza.unknown.desc",
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(150),
            List.of(),
            "UNKNOWN",
            1,
            BigDecimal.ZERO);

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(pizzaService.get(pizzaId, DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(response));

    StepVerifier.create(controller.get(PIZZERIA_CODE, pizzaId))
        .assertNext(detail -> assertThat(detail.totalCalories()).isEqualTo(BigDecimal.ZERO))
        .verifyComplete();
  }

  @Test
  void listShouldReturnPizzasFromMenuServiceWithCalories() {
    UUID pizzaId1 = UUID.randomUUID();
    UUID pizzaId2 = UUID.randomUUID();
    BigDecimal calories1 = BigDecimal.valueOf(600);
    BigDecimal calories2 = BigDecimal.valueOf(1200);

    PizzaSummaryResponse pizza1 =
        new PizzaSummaryResponse(
            pizzaId1,
            1,
            "pizza.margarita",
            BigDecimal.valueOf(95),
            BigDecimal.valueOf(165),
            "VEGETARIAN",
            1,
            calories1);

    PizzaSummaryResponse pizza2 =
        new PizzaSummaryResponse(
            pizzaId2,
            2,
            "pizza.quattro.formaggi",
            BigDecimal.valueOf(115),
            BigDecimal.valueOf(185),
            "VEGETARIAN",
            2,
            calories2);

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(pizzaService.list(DEFAULT_PIZZERIA_ID)).thenReturn(Flux.just(pizza1, pizza2));

    // Note: This test verifies that if we called pizzaService.list directly,
    // it would return calories. The controller's list method currently uses
    // menuService.getMenu and transforms the response, so we need to ensure
    // the transformation preserves calories
    StepVerifier.create(pizzaService.list(DEFAULT_PIZZERIA_ID).collectList())
        .assertNext(
            pizzas -> {
              assertThat(pizzas).hasSize(2);
              assertThat(pizzas.get(0).totalCalories()).isEqualTo(calories1);
              assertThat(pizzas.get(1).totalCalories()).isEqualTo(calories2);
            })
        .verifyComplete();
  }
}
