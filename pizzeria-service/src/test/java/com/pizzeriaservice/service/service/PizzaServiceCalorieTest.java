package com.pizzeriaservice.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.api.dto.MenuIngredientResponse;
import com.pizzeriaservice.service.menu.MenuIngredientFactEntity;
import com.pizzeriaservice.service.menu.MenuIngredientFactRepository;
import com.pizzeriaservice.service.menu.MenuItemEntity;
import com.pizzeriaservice.service.menu.MenuItemIngredientEntity;
import com.pizzeriaservice.service.menu.MenuItemIngredientRepository;
import com.pizzeriaservice.service.menu.MenuItemRepository;
import com.pizzeriaservice.service.menu.MenuSectionEntity;
import com.pizzeriaservice.service.menu.MenuSectionRepository;
import com.pizzeriaservice.service.menu.PizzaCustomisationRepository;
import com.pizzeriaservice.service.repository.UserRepository;
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

class PizzaServiceCalorieTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private MenuSectionRepository sectionRepository;
  @Mock private MenuItemRepository itemRepository;
  @Mock private MenuItemIngredientRepository ingredientRepository;
  @Mock private MenuIngredientFactRepository ingredientFactRepository;
  @Mock private PizzaCustomisationRepository customisationRepository;
  @Mock private UserRepository userRepository;

  private PizzaService pizzaService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    pizzaService =
        new PizzaService(
            sectionRepository,
            itemRepository,
            ingredientRepository,
            ingredientFactRepository,
            customisationRepository,
            userRepository);
  }

  @Test
  void listShouldReturnPizzasWithCalories() {
    UUID sectionId = UUID.randomUUID();
    UUID pizzaId = UUID.randomUUID();
    BigDecimal totalCalories = BigDecimal.valueOf(850.50);

    MenuSectionEntity section =
        new MenuSectionEntity(
            sectionId,
            DEFAULT_PIZZERIA_ID,
            "pizza_classics",
            "translation.key.section.pizza.classics",
            1,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    MenuItemEntity pizza =
        new MenuItemEntity(
            pizzaId,
            DEFAULT_PIZZERIA_ID,
            sectionId,
            1,
            "translation.key.disc.pizza.margarita",
            "translation.key.disc.desc.pizza.margarita",
            new BigDecimal("95.00"),
            new BigDecimal("165.00"),
            1,
            totalCalories,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    when(sectionRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.just(section));
    when(itemRepository.findAllBySectionIdOrderBySortOrderAsc(sectionId))
        .thenReturn(Flux.just(pizza));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(pizzaId))
        .thenReturn(Flux.empty());

    StepVerifier.create(pizzaService.list(DEFAULT_PIZZERIA_ID))
        .assertNext(
            summary -> {
              assertThat(summary.id()).isEqualTo(pizzaId);
              assertThat(summary.totalCalories()).isEqualTo(totalCalories);
              assertThat(summary.totalCalories()).isGreaterThan(BigDecimal.valueOf(800));
            })
        .verifyComplete();
  }

  @Test
  void listShouldReturnMultiplePizzasWithDifferentCalories() {
    UUID sectionId = UUID.randomUUID();
    UUID pizzaId1 = UUID.randomUUID();
    UUID pizzaId2 = UUID.randomUUID();

    MenuSectionEntity section =
        new MenuSectionEntity(
            sectionId,
            DEFAULT_PIZZERIA_ID,
            "pizza_classics",
            "translation.key.section.pizza.classics",
            1,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    MenuItemEntity lowCaloriePizza =
        new MenuItemEntity(
            pizzaId1,
            DEFAULT_PIZZERIA_ID,
            sectionId,
            1,
            "translation.key.disc.pizza.margarita",
            null,
            new BigDecimal("95.00"),
            new BigDecimal("165.00"),
            1,
            BigDecimal.valueOf(600),
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    MenuItemEntity highCaloriePizza =
        new MenuItemEntity(
            pizzaId2,
            DEFAULT_PIZZERIA_ID,
            sectionId,
            2,
            "translation.key.disc.pizza.quattro.formaggi",
            null,
            new BigDecimal("115.00"),
            new BigDecimal("185.00"),
            2,
            BigDecimal.valueOf(1200),
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    when(sectionRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.just(section));
    when(itemRepository.findAllBySectionIdOrderBySortOrderAsc(sectionId))
        .thenReturn(Flux.just(lowCaloriePizza, highCaloriePizza));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(pizzaId1))
        .thenReturn(Flux.empty());
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(pizzaId2))
        .thenReturn(Flux.empty());

    StepVerifier.create(pizzaService.list(DEFAULT_PIZZERIA_ID).collectList())
        .assertNext(
            pizzas -> {
              assertThat(pizzas).hasSize(2);
              assertThat(pizzas.get(0).totalCalories()).isEqualTo(BigDecimal.valueOf(600));
              assertThat(pizzas.get(1).totalCalories()).isEqualTo(BigDecimal.valueOf(1200));
              assertThat(pizzas.get(0).totalCalories()).isLessThan(pizzas.get(1).totalCalories());
            })
        .verifyComplete();
  }

  @Test
  void getShouldReturnPizzaDetailWithCalories() {
    UUID pizzaId = UUID.randomUUID();
    BigDecimal totalCalories = BigDecimal.valueOf(920.75);

    MenuItemEntity pizza =
        new MenuItemEntity(
            pizzaId,
            DEFAULT_PIZZERIA_ID,
            UUID.randomUUID(),
            1,
            "translation.key.disc.pizza.pepperoni",
            "translation.key.disc.desc.pizza.pepperoni",
            new BigDecimal("105.00"),
            new BigDecimal("175.00"),
            1,
            totalCalories,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    when(itemRepository.findByIdAndPizzeriaId(pizzaId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(pizza));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(pizzaId))
        .thenReturn(Flux.empty());

    StepVerifier.create(pizzaService.get(pizzaId, DEFAULT_PIZZERIA_ID))
        .assertNext(
            detail -> {
              assertThat(detail.id()).isEqualTo(pizzaId);
              assertThat(detail.totalCalories()).isEqualTo(totalCalories);
              assertThat(detail.totalCalories()).isGreaterThan(BigDecimal.valueOf(900));
            })
        .verifyComplete();
  }

  @Test
  void getShouldReturnPizzaWithIngredientCalories() {
    UUID pizzaId = UUID.randomUUID();
    UUID ingredientFactId = UUID.randomUUID();
    BigDecimal totalCalories = BigDecimal.valueOf(850);
    BigDecimal cheeseCalories = BigDecimal.valueOf(280);

    MenuItemEntity pizza =
        new MenuItemEntity(
            pizzaId,
            DEFAULT_PIZZERIA_ID,
            UUID.randomUUID(),
            1,
            "translation.key.disc.pizza.margarita",
            "translation.key.disc.desc.pizza.margarita",
            new BigDecimal("95.00"),
            new BigDecimal("165.00"),
            1,
            totalCalories,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    MenuItemIngredientEntity ingredient =
        new MenuItemIngredientEntity(
            UUID.randomUUID(), pizzaId, "translation.key.ingredient.mozzarella", 1);

    MenuIngredientFactEntity ingredientFact =
        new MenuIngredientFactEntity(
            ingredientFactId,
            DEFAULT_PIZZERIA_ID,
            "translation.key.ingredient.mozzarella",
            "VEGETARIAN",
            "DAIRY",
            0,
            cheeseCalories);

    when(itemRepository.findByIdAndPizzeriaId(pizzaId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(pizza));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(pizzaId))
        .thenReturn(Flux.just(ingredient));
    when(ingredientFactRepository.findByIngredientKeyAndPizzeriaId(
            "translation.key.ingredient.mozzarella", DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(ingredientFact));

    StepVerifier.create(pizzaService.get(pizzaId, DEFAULT_PIZZERIA_ID))
        .assertNext(
            detail -> {
              assertThat(detail.totalCalories()).isEqualTo(totalCalories);
              assertThat(detail.ingredients()).hasSize(1);

              MenuIngredientResponse ingredientResponse = detail.ingredients().get(0);
              assertThat(ingredientResponse.caloriesPer100g()).isEqualTo(cheeseCalories);
            })
        .verifyComplete();
  }

  @Test
  void getShouldHandleZeroCaloriesForPizza() {
    UUID pizzaId = UUID.randomUUID();

    MenuItemEntity pizza =
        new MenuItemEntity(
            pizzaId,
            DEFAULT_PIZZERIA_ID,
            UUID.randomUUID(),
            1,
            "translation.key.disc.pizza.unknown",
            null,
            new BigDecimal("100.00"),
            new BigDecimal("150.00"),
            1,
            BigDecimal.ZERO,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    when(itemRepository.findByIdAndPizzeriaId(pizzaId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(pizza));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(pizzaId))
        .thenReturn(Flux.empty());

    StepVerifier.create(pizzaService.get(pizzaId, DEFAULT_PIZZERIA_ID))
        .assertNext(detail -> assertThat(detail.totalCalories()).isEqualTo(BigDecimal.ZERO))
        .verifyComplete();
  }

  @Test
  void listShouldHandlePizzasWithZeroCalories() {
    UUID sectionId = UUID.randomUUID();
    UUID pizzaId = UUID.randomUUID();

    MenuSectionEntity section =
        new MenuSectionEntity(
            sectionId,
            DEFAULT_PIZZERIA_ID,
            "pizza_specials",
            "translation.key.section.pizza.specials",
            1,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    MenuItemEntity pizza =
        new MenuItemEntity(
            pizzaId,
            DEFAULT_PIZZERIA_ID,
            sectionId,
            1,
            "translation.key.disc.pizza.new",
            null,
            new BigDecimal("100.00"),
            new BigDecimal("150.00"),
            1,
            BigDecimal.ZERO,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    when(sectionRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.just(section));
    when(itemRepository.findAllBySectionIdOrderBySortOrderAsc(sectionId))
        .thenReturn(Flux.just(pizza));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(pizzaId))
        .thenReturn(Flux.empty());

    StepVerifier.create(pizzaService.list(DEFAULT_PIZZERIA_ID))
        .assertNext(
            summary -> {
              assertThat(summary.id()).isEqualTo(pizzaId);
              assertThat(summary.totalCalories()).isEqualTo(BigDecimal.ZERO);
            })
        .verifyComplete();
  }
}
