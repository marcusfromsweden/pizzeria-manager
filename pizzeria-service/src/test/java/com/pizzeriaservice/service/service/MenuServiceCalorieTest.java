package com.pizzeriaservice.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.api.dto.MenuIngredientResponse;
import com.pizzeriaservice.api.dto.MenuItemResponse;
import com.pizzeriaservice.service.menu.MenuIngredientFactEntity;
import com.pizzeriaservice.service.menu.MenuIngredientFactRepository;
import com.pizzeriaservice.service.menu.MenuItemEntity;
import com.pizzeriaservice.service.menu.MenuItemIngredientEntity;
import com.pizzeriaservice.service.menu.MenuItemIngredientRepository;
import com.pizzeriaservice.service.menu.MenuItemRepository;
import com.pizzeriaservice.service.menu.MenuSectionEntity;
import com.pizzeriaservice.service.menu.MenuSectionRepository;
import com.pizzeriaservice.service.menu.PizzaCustomisationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class MenuServiceCalorieTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private MenuSectionRepository sectionRepository;
  @Mock private MenuItemRepository itemRepository;
  @Mock private MenuItemIngredientRepository ingredientRepository;
  @Mock private MenuIngredientFactRepository ingredientFactRepository;
  @Mock private PizzaCustomisationRepository customisationRepository;

  private MenuService menuService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    menuService =
        new MenuService(
            sectionRepository,
            itemRepository,
            ingredientRepository,
            ingredientFactRepository,
            customisationRepository);
  }

  @Test
  void getMenuShouldReturnCaloriesForMenuItem() {
    UUID sectionId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    BigDecimal totalCalories = BigDecimal.valueOf(850.50);

    MenuSectionEntity section =
        new MenuSectionEntity(
            sectionId,
            DEFAULT_PIZZERIA_ID,
            "pizzas",
            "translation.key.section.pizzas",
            1,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    MenuItemEntity item =
        new MenuItemEntity(
            itemId,
            DEFAULT_PIZZERIA_ID,
            sectionId,
            1,
            "translation.key.disc.pizza.margarita",
            null,
            new BigDecimal("100.00"),
            new BigDecimal("250.00"),
            1,
            totalCalories,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    when(sectionRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.just(section));
    when(itemRepository.findAllBySectionIdOrderBySortOrderAsc(sectionId))
        .thenReturn(Flux.just(item));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(itemId))
        .thenReturn(Flux.empty());
    when(customisationRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.empty());

    StepVerifier.create(menuService.getMenu(DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              assertThat(response.sections()).hasSize(1);
              MenuItemResponse itemResponse = response.sections().get(0).items().get(0);
              assertThat(itemResponse.totalCalories()).isEqualTo(totalCalories);
              assertThat(itemResponse.totalCalories())
                  .isGreaterThan(BigDecimal.valueOf(800))
                  .isLessThan(BigDecimal.valueOf(900));
            })
        .verifyComplete();
  }

  @Test
  void getMenuShouldReturnCaloriesForIngredients() {
    UUID sectionId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();
    UUID ingredientId = UUID.randomUUID();
    BigDecimal ingredientCalories = BigDecimal.valueOf(280.00);

    MenuSectionEntity section =
        new MenuSectionEntity(
            sectionId,
            DEFAULT_PIZZERIA_ID,
            "pizzas",
            "translation.key.section.pizzas",
            1,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    MenuItemEntity item =
        new MenuItemEntity(
            itemId,
            DEFAULT_PIZZERIA_ID,
            sectionId,
            1,
            "translation.key.disc.pizza.margarita",
            null,
            new BigDecimal("100.00"),
            new BigDecimal("250.00"),
            1,
            BigDecimal.valueOf(850),
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    MenuItemIngredientEntity ingredient =
        new MenuItemIngredientEntity(
            UUID.randomUUID(), itemId, "translation.key.ingredient.mozzarella", 1);

    MenuIngredientFactEntity ingredientFact =
        new MenuIngredientFactEntity(
            ingredientId,
            DEFAULT_PIZZERIA_ID,
            "translation.key.ingredient.mozzarella",
            "VEGETARIAN",
            "DAIRY",
            0,
            ingredientCalories);

    when(sectionRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.just(section));
    when(itemRepository.findAllBySectionIdOrderBySortOrderAsc(sectionId))
        .thenReturn(Flux.just(item));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(itemId))
        .thenReturn(Flux.just(ingredient));
    when(ingredientFactRepository.findByIngredientKeyAndPizzeriaId(
            "translation.key.ingredient.mozzarella", DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(ingredientFact));
    when(customisationRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.empty());

    StepVerifier.create(menuService.getMenu(DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              assertThat(response.sections()).hasSize(1);
              MenuItemResponse itemResponse = response.sections().get(0).items().get(0);
              assertThat(itemResponse.ingredients()).hasSize(1);

              MenuIngredientResponse ingredientResponse = itemResponse.ingredients().get(0);
              assertThat(ingredientResponse.caloriesPer100g()).isEqualTo(ingredientCalories);
              assertThat(ingredientResponse.caloriesPer100g())
                  .isGreaterThan(BigDecimal.valueOf(200));
            })
        .verifyComplete();
  }

  @Test
  void getMenuShouldHandleZeroCalories() {
    UUID sectionId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();

    MenuSectionEntity section =
        new MenuSectionEntity(
            sectionId,
            DEFAULT_PIZZERIA_ID,
            "pizzas",
            "translation.key.section.pizzas",
            1,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    MenuItemEntity item =
        new MenuItemEntity(
            itemId,
            DEFAULT_PIZZERIA_ID,
            sectionId,
            1,
            "translation.key.disc.pizza.unknown",
            null,
            new BigDecimal("100.00"),
            new BigDecimal("250.00"),
            1,
            BigDecimal.ZERO,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    when(sectionRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.just(section));
    when(itemRepository.findAllBySectionIdOrderBySortOrderAsc(sectionId))
        .thenReturn(Flux.just(item));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(itemId))
        .thenReturn(Flux.empty());
    when(customisationRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.empty());

    StepVerifier.create(menuService.getMenu(DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              assertThat(response.sections()).hasSize(1);
              MenuItemResponse itemResponse = response.sections().get(0).items().get(0);
              assertThat(itemResponse.totalCalories()).isEqualTo(BigDecimal.ZERO);
            })
        .verifyComplete();
  }

  @Test
  void getMenuShouldHandleMissingIngredientFactWithZeroCalories() {
    UUID sectionId = UUID.randomUUID();
    UUID itemId = UUID.randomUUID();

    MenuSectionEntity section =
        new MenuSectionEntity(
            sectionId,
            DEFAULT_PIZZERIA_ID,
            "pizzas",
            "translation.key.section.pizzas",
            1,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    MenuItemEntity item =
        new MenuItemEntity(
            itemId,
            DEFAULT_PIZZERIA_ID,
            sectionId,
            1,
            "translation.key.disc.pizza.special",
            null,
            new BigDecimal("100.00"),
            new BigDecimal("250.00"),
            1,
            BigDecimal.valueOf(500),
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    MenuItemIngredientEntity ingredient =
        new MenuItemIngredientEntity(
            UUID.randomUUID(), itemId, "translation.key.ingredient.unknown", 1);

    when(sectionRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.just(section));
    when(itemRepository.findAllBySectionIdOrderBySortOrderAsc(sectionId))
        .thenReturn(Flux.just(item));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(itemId))
        .thenReturn(Flux.just(ingredient));
    when(ingredientFactRepository.findByIngredientKeyAndPizzeriaId(
            "translation.key.ingredient.unknown", DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.empty());
    when(customisationRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.empty());

    StepVerifier.create(menuService.getMenu(DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              assertThat(response.sections()).hasSize(1);
              MenuItemResponse itemResponse = response.sections().get(0).items().get(0);
              assertThat(itemResponse.ingredients()).hasSize(1);

              MenuIngredientResponse ingredientResponse = itemResponse.ingredients().get(0);
              // When ingredient fact is missing, fallback should have ZERO calories
              assertThat(ingredientResponse.caloriesPer100g()).isEqualTo(BigDecimal.ZERO);
            })
        .verifyComplete();
  }

  @Test
  void getMenuItemShouldReturnCalories() {
    UUID itemId = UUID.randomUUID();
    BigDecimal totalCalories = BigDecimal.valueOf(920.75);

    MenuItemEntity item =
        new MenuItemEntity(
            itemId,
            DEFAULT_PIZZERIA_ID,
            UUID.randomUUID(),
            1,
            "translation.key.disc.pizza.quattro.formaggi",
            "translation.key.disc.desc.quattro.formaggi",
            new BigDecimal("115.00"),
            new BigDecimal("185.00"),
            1,
            totalCalories,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    when(itemRepository.findByIdAndPizzeriaId(itemId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(item));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(itemId))
        .thenReturn(Flux.empty());

    StepVerifier.create(menuService.getMenuItem(itemId, DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              assertThat(response.id()).isEqualTo(itemId);
              assertThat(response.totalCalories()).isEqualTo(totalCalories);
            })
        .verifyComplete();
  }
}
