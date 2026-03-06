package com.pizzeriaservice.service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.api.dto.MenuIngredientResponse;
import com.pizzeriaservice.api.dto.MenuItemResponse;
import com.pizzeriaservice.api.dto.MenuResponse;
import com.pizzeriaservice.api.dto.MenuSectionResponse;
import com.pizzeriaservice.service.service.MenuService;
import com.pizzeriaservice.service.service.PizzeriaService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class MenuControllerCalorieTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final String PIZZERIA_CODE = "testpizzeria";

  @Mock private MenuService menuService;
  @Mock private PizzeriaService pizzeriaService;

  private MenuController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new MenuController(menuService, pizzeriaService);
  }

  @Test
  void getMenuShouldReturnMenuItemsWithCalories() {
    UUID itemId = UUID.randomUUID();
    BigDecimal totalCalories = BigDecimal.valueOf(850.50);

    MenuItemResponse item =
        new MenuItemResponse(
            itemId,
            UUID.randomUUID(),
            1,
            "pizza.margarita",
            "pizza.margarita.desc",
            BigDecimal.valueOf(95),
            BigDecimal.valueOf(165),
            List.of(),
            "VEGETARIAN",
            1,
            totalCalories);

    MenuSectionResponse section =
        new MenuSectionResponse(
            UUID.randomUUID(), "pizzas", "menu.section.pizzas", 1, List.of(item));

    MenuResponse menuResponse = new MenuResponse(List.of(section), List.of());

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(menuService.getMenu(DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(menuResponse));

    StepVerifier.create(controller.getMenu(PIZZERIA_CODE))
        .assertNext(
            response -> {
              assertThat(response.sections()).hasSize(1);
              MenuItemResponse itemResponse = response.sections().get(0).items().get(0);
              assertThat(itemResponse.totalCalories()).isEqualTo(totalCalories);
              assertThat(itemResponse.totalCalories()).isGreaterThan(BigDecimal.valueOf(800));
            })
        .verifyComplete();

    verify(pizzeriaService).resolvePizzeriaId(PIZZERIA_CODE);
    verify(menuService).getMenu(DEFAULT_PIZZERIA_ID);
  }

  @Test
  void getMenuShouldReturnIngredientsWithCalories() {
    UUID itemId = UUID.randomUUID();
    UUID ingredientId = UUID.randomUUID();
    BigDecimal ingredientCalories = BigDecimal.valueOf(280.00);

    MenuIngredientResponse ingredient =
        new MenuIngredientResponse(
            ingredientId,
            "ingredient.mozzarella",
            "VEGETARIAN",
            Set.of("DAIRY"),
            0,
            ingredientCalories);

    MenuItemResponse item =
        new MenuItemResponse(
            itemId,
            UUID.randomUUID(),
            1,
            "pizza.margarita",
            "pizza.margarita.desc",
            BigDecimal.valueOf(95),
            BigDecimal.valueOf(165),
            List.of(ingredient),
            "VEGETARIAN",
            1,
            BigDecimal.valueOf(850));

    MenuSectionResponse section =
        new MenuSectionResponse(
            UUID.randomUUID(), "pizzas", "menu.section.pizzas", 1, List.of(item));

    MenuResponse menuResponse = new MenuResponse(List.of(section), List.of());

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(menuService.getMenu(DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(menuResponse));

    StepVerifier.create(controller.getMenu(PIZZERIA_CODE))
        .assertNext(
            response -> {
              MenuItemResponse itemResponse = response.sections().get(0).items().get(0);
              assertThat(itemResponse.ingredients()).hasSize(1);

              MenuIngredientResponse ingredientResponse = itemResponse.ingredients().get(0);
              assertThat(ingredientResponse.caloriesPer100g()).isEqualTo(ingredientCalories);
            })
        .verifyComplete();
  }

  @Test
  void getMenuShouldHandleZeroCaloriesForItems() {
    UUID itemId = UUID.randomUUID();

    MenuItemResponse item =
        new MenuItemResponse(
            itemId,
            UUID.randomUUID(),
            1,
            "pizza.unknown",
            "pizza.unknown.desc",
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(150),
            List.of(),
            "UNKNOWN",
            1,
            BigDecimal.ZERO);

    MenuSectionResponse section =
        new MenuSectionResponse(
            UUID.randomUUID(), "pizzas", "menu.section.pizzas", 1, List.of(item));

    MenuResponse menuResponse = new MenuResponse(List.of(section), List.of());

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(menuService.getMenu(DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(menuResponse));

    StepVerifier.create(controller.getMenu(PIZZERIA_CODE))
        .assertNext(
            response -> {
              MenuItemResponse itemResponse = response.sections().get(0).items().get(0);
              assertThat(itemResponse.totalCalories()).isEqualTo(BigDecimal.ZERO);
            })
        .verifyComplete();
  }

  @Test
  void getMenuShouldHandleMultipleItemsWithDifferentCalories() {
    UUID item1Id = UUID.randomUUID();
    UUID item2Id = UUID.randomUUID();

    MenuItemResponse lowCalorieItem =
        new MenuItemResponse(
            item1Id,
            UUID.randomUUID(),
            1,
            "pizza.margarita",
            "pizza.margarita.desc",
            BigDecimal.valueOf(95),
            BigDecimal.valueOf(165),
            List.of(),
            "VEGETARIAN",
            1,
            BigDecimal.valueOf(600));

    MenuItemResponse highCalorieItem =
        new MenuItemResponse(
            item2Id,
            UUID.randomUUID(),
            2,
            "pizza.quattro.formaggi",
            "pizza.quattro.formaggi.desc",
            BigDecimal.valueOf(115),
            BigDecimal.valueOf(185),
            List.of(),
            "VEGETARIAN",
            2,
            BigDecimal.valueOf(1200));

    MenuSectionResponse section =
        new MenuSectionResponse(
            UUID.randomUUID(),
            "pizzas",
            "menu.section.pizzas",
            1,
            List.of(lowCalorieItem, highCalorieItem));

    MenuResponse menuResponse = new MenuResponse(List.of(section), List.of());

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(menuService.getMenu(DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(menuResponse));

    StepVerifier.create(controller.getMenu(PIZZERIA_CODE))
        .assertNext(
            response -> {
              List<MenuItemResponse> items = response.sections().get(0).items();
              assertThat(items).hasSize(2);
              assertThat(items.get(0).totalCalories()).isEqualTo(BigDecimal.valueOf(600));
              assertThat(items.get(1).totalCalories()).isEqualTo(BigDecimal.valueOf(1200));
            })
        .verifyComplete();
  }
}
