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

class MenuServiceTest {

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
  void getMenuMapsIngredientFacts() {
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
            "translation.key.disc.pizza.margarita",
            null,
            new BigDecimal("100.00"),
            new BigDecimal("250.00"),
            1,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    MenuItemIngredientEntity ingredient =
        new MenuItemIngredientEntity(
            UUID.randomUUID(), itemId, "translation.key.ingredient.cheese", 1);

    when(sectionRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.just(section));
    when(itemRepository.findAllBySectionIdOrderBySortOrderAsc(sectionId))
        .thenReturn(Flux.just(item));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(itemId))
        .thenReturn(Flux.just(ingredient));
    when(ingredientFactRepository.findByIngredientKeyAndPizzeriaId(
            "translation.key.ingredient.cheese", DEFAULT_PIZZERIA_ID))
        .thenReturn(
            Mono.just(
                new MenuIngredientFactEntity(
                    UUID.randomUUID(),
                    DEFAULT_PIZZERIA_ID,
                    "translation.key.ingredient.cheese",
                    "VEGETARIAN",
                    "DAIRY",
                    0)));
    when(customisationRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.empty());

    StepVerifier.create(menuService.getMenu(DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              assertThat(response.sections()).hasSize(1);
              var itemResponse = response.sections().get(0).items().get(0);
              assertThat(itemResponse.ingredients()).hasSize(1);
              MenuIngredientResponse ingredientResponse = itemResponse.ingredients().get(0);
              assertThat(ingredientResponse.id()).isNotNull();
              assertThat(ingredientResponse.ingredientKey())
                  .isEqualTo("translation.key.ingredient.cheese");
              assertThat(ingredientResponse.dietaryType()).isEqualTo("VEGETARIAN");
              assertThat(ingredientResponse.allergenTags()).containsExactly("DAIRY");
            })
        .verifyComplete();
  }

  @Test
  void getMenuUsesFallbackWhenFactMissing() {
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
            "translation.key.disc.pizza.margarita",
            null,
            new BigDecimal("100.00"),
            new BigDecimal("250.00"),
            1,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    MenuItemIngredientEntity ingredient =
        new MenuItemIngredientEntity(
            UUID.randomUUID(), itemId, "translation.key.ingredient.basil", 1);

    when(sectionRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.just(section));
    when(itemRepository.findAllBySectionIdOrderBySortOrderAsc(sectionId))
        .thenReturn(Flux.just(item));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(itemId))
        .thenReturn(Flux.just(ingredient));
    when(ingredientFactRepository.findByIngredientKeyAndPizzeriaId(
            "translation.key.ingredient.basil", DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.empty());
    when(customisationRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.empty());

    StepVerifier.create(menuService.getMenu(DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              MenuIngredientResponse ingredientResponse =
                  response.sections().get(0).items().get(0).ingredients().get(0);
              assertThat(ingredientResponse.id()).isNull();
              assertThat(ingredientResponse.ingredientKey())
                  .isEqualTo("translation.key.ingredient.basil");
              assertThat(ingredientResponse.dietaryType()).isEqualTo("UNKNOWN");
              assertThat(ingredientResponse.allergenTags()).isEmpty();
              assertThat(ingredientResponse.spiceLevel()).isZero();
            })
        .verifyComplete();
  }
}
