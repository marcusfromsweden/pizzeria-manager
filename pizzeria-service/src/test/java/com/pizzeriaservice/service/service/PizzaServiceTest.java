package com.pizzeriaservice.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.api.dto.PizzaSuitabilityRequest;
import com.pizzeriaservice.service.domain.Diet;
import com.pizzeriaservice.service.domain.PizzaSize;
import com.pizzeriaservice.service.domain.model.User;
import com.pizzeriaservice.service.menu.MenuIngredientFactEntity;
import com.pizzeriaservice.service.menu.MenuIngredientFactRepository;
import com.pizzeriaservice.service.menu.MenuItemEntity;
import com.pizzeriaservice.service.menu.MenuItemIngredientEntity;
import com.pizzeriaservice.service.menu.MenuItemIngredientRepository;
import com.pizzeriaservice.service.menu.MenuItemRepository;
import com.pizzeriaservice.service.menu.MenuSectionEntity;
import com.pizzeriaservice.service.menu.MenuSectionRepository;
import com.pizzeriaservice.service.menu.PizzaCustomisationEntity;
import com.pizzeriaservice.service.menu.PizzaCustomisationRepository;
import com.pizzeriaservice.service.repository.UserRepository;
import com.pizzeriaservice.service.support.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
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

class PizzaServiceTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID OTHER_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Mock private MenuSectionRepository sectionRepository;
  @Mock private MenuItemRepository itemRepository;
  @Mock private MenuItemIngredientRepository ingredientRepository;
  @Mock private MenuIngredientFactRepository ingredientFactRepository;
  @Mock private PizzaCustomisationRepository customisationRepository;
  @Mock private UserRepository userRepository;

  private PizzaService pizzaService;

  private UUID pizzaId;
  private UUID userId;
  private UUID cheeseFactId;
  private UUID pepperoniFactId;

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

    pizzaId = UUID.randomUUID();
    userId = UUID.randomUUID();
    cheeseFactId = UUID.randomUUID();
    pepperoniFactId = UUID.randomUUID();
  }

  @Test
  void suitabilityFlagsDietViolationsAndSuggestions() {
    UUID cheeseIngredientId = UUID.randomUUID();
    UUID pepperoniIngredientId = UUID.randomUUID();

    MenuItemEntity pizza =
        new MenuItemEntity(
            pizzaId,
            DEFAULT_PIZZERIA_ID,
            UUID.randomUUID(),
            1,
            "translation.key.disc.pizza.pepperoni",
            null,
            new BigDecimal("120.00"),
            new BigDecimal("280.00"),
            1,
            BigDecimal.ZERO,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    when(itemRepository.findByIdAndPizzeriaId(pizzaId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(pizza));

    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(pizzaId))
        .thenReturn(
            Flux.just(
                new MenuItemIngredientEntity(
                    UUID.randomUUID(), pizzaId, "translation.key.ingredient.cheese", 1),
                new MenuItemIngredientEntity(
                    UUID.randomUUID(), pizzaId, "translation.key.ingredient.pepperoni", 2)));

    when(ingredientFactRepository.findByIngredientKeyAndPizzeriaId(
            "translation.key.ingredient.cheese", DEFAULT_PIZZERIA_ID))
        .thenReturn(
            Mono.just(
                new MenuIngredientFactEntity(
                    cheeseFactId,
                    DEFAULT_PIZZERIA_ID,
                    "translation.key.ingredient.cheese",
                    "VEGETARIAN",
                    "DAIRY",
                    0,
                    BigDecimal.ZERO)));

    when(ingredientFactRepository.findByIngredientKeyAndPizzeriaId(
            "translation.key.ingredient.pepperoni", DEFAULT_PIZZERIA_ID))
        .thenReturn(
            Mono.just(
                new MenuIngredientFactEntity(
                    pepperoniFactId,
                    DEFAULT_PIZZERIA_ID,
                    "translation.key.ingredient.pepperoni",
                    "CARNIVORE",
                    "",
                    0,
                    BigDecimal.ZERO)));

    when(userRepository.findByIdAndPizzeriaId(userId, DEFAULT_PIZZERIA_ID))
        .thenReturn(
            Mono.just(
                User.builder()
                    .id(userId)
                    .pizzeriaId(DEFAULT_PIZZERIA_ID)
                    .name("Vegan User")
                    .email("vegan@example.com")
                    .preferredDiet(Diet.VEGAN)
                    .preferredIngredientIds(Set.of(cheeseFactId))
                    .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .updatedAt(Instant.parse("2024-01-01T00:00:00Z"))
                    .build()));

    when(ingredientFactRepository.findAllByIdInAndPizzeriaId(
            org.mockito.ArgumentMatchers.<java.util.Collection<UUID>>any(),
            org.mockito.ArgumentMatchers.eq(DEFAULT_PIZZERIA_ID)))
        .thenAnswer(
            invocation -> {
              Iterable<UUID> ids = invocation.getArgument(0);
              java.util.List<MenuIngredientFactEntity> facts = new java.util.ArrayList<>();
              ids.forEach(
                  id -> {
                    if (pepperoniFactId.equals(id)) {
                      facts.add(
                          new MenuIngredientFactEntity(
                              pepperoniFactId,
                              DEFAULT_PIZZERIA_ID,
                              "translation.key.ingredient.pepperoni",
                              "CARNIVORE",
                              "",
                              0,
                              BigDecimal.ZERO));
                    } else if (cheeseFactId.equals(id)) {
                      facts.add(
                          new MenuIngredientFactEntity(
                              cheeseFactId,
                              DEFAULT_PIZZERIA_ID,
                              "translation.key.ingredient.cheese",
                              "VEGETARIAN",
                              "DAIRY",
                              0,
                              BigDecimal.ZERO));
                    }
                  });
              return Flux.fromIterable(facts);
            });

    PizzaSuitabilityRequest request =
        new PizzaSuitabilityRequest(pizzaId, List.of(pepperoniFactId), List.of());

    StepVerifier.create(pizzaService.suitability(request, userId, DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              assertThat(response.suitable()).isFalse();
              assertThat(response.violations())
                  .anyMatch(message -> message.contains("violates vegan"));
              assertThat(response.suggestions()).isNotNull();
            })
        .verifyComplete();
  }

  @Test
  void priceForCustomisationsSumsMatchingEntries() {
    UUID customisationOne = UUID.randomUUID();
    UUID customisationTwo = UUID.randomUUID();

    when(customisationRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(
            Flux.just(
                new PizzaCustomisationEntity(
                    customisationOne,
                    DEFAULT_PIZZERIA_ID,
                    "translation.key.customisation.extra.cheese",
                    new BigDecimal("25.00"),
                    null,
                    1,
                    Instant.parse("2024-01-01T00:00:00Z"),
                    Instant.parse("2024-01-01T00:00:00Z")),
                new PizzaCustomisationEntity(
                    UUID.randomUUID(),
                    DEFAULT_PIZZERIA_ID,
                    "translation.key.customisation.extra.basil",
                    new BigDecimal("10.00"),
                    null,
                    2,
                    Instant.parse("2024-01-01T00:00:00Z"),
                    Instant.parse("2024-01-01T00:00:00Z")),
                new PizzaCustomisationEntity(
                    customisationTwo,
                    DEFAULT_PIZZERIA_ID,
                    "translation.key.customisation.extra.pepperoni",
                    new BigDecimal("30.00"),
                    null,
                    3,
                    Instant.parse("2024-01-01T00:00:00Z"),
                    Instant.parse("2024-01-01T00:00:00Z"))));

    StepVerifier.create(
            pizzaService.priceForCustomisations(
                List.of(customisationOne, customisationTwo),
                DEFAULT_PIZZERIA_ID,
                PizzaSize.REGULAR))
        .expectNext(new BigDecimal("55.00"))
        .verifyComplete();
  }

  @Test
  void priceForCustomisationsReturnsZeroWhenIdsEmpty() {
    StepVerifier.create(
            pizzaService.priceForCustomisations(List.of(), DEFAULT_PIZZERIA_ID, PizzaSize.REGULAR))
        .expectNext(BigDecimal.ZERO)
        .verifyComplete();

    StepVerifier.create(
            pizzaService.priceForCustomisations(null, DEFAULT_PIZZERIA_ID, PizzaSize.REGULAR))
        .expectNext(BigDecimal.ZERO)
        .verifyComplete();
  }

  // Cross-tenant isolation tests

  @Test
  void getFailsWhenAccessingPizzaFromDifferentPizzeria() {
    // Pizza exists in DEFAULT_PIZZERIA
    MenuItemEntity pizza =
        new MenuItemEntity(
            pizzaId,
            DEFAULT_PIZZERIA_ID,
            UUID.randomUUID(),
            1,
            "translation.key.pizza.margherita",
            null,
            new BigDecimal("100.00"),
            new BigDecimal("200.00"),
            1,
            BigDecimal.ZERO,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    // Mock returns pizza only for DEFAULT_PIZZERIA
    when(itemRepository.findByIdAndPizzeriaId(pizzaId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(pizza));
    // Returns empty for OTHER_PIZZERIA
    when(itemRepository.findByIdAndPizzeriaId(pizzaId, OTHER_PIZZERIA_ID)).thenReturn(Mono.empty());

    // Accessing from OTHER_PIZZERIA should fail
    StepVerifier.create(pizzaService.get(pizzaId, OTHER_PIZZERIA_ID))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  @Test
  void suitabilityFailsWhenUserIsFromDifferentPizzeria() {
    // Pizza exists in DEFAULT_PIZZERIA
    MenuItemEntity pizza =
        new MenuItemEntity(
            pizzaId,
            DEFAULT_PIZZERIA_ID,
            UUID.randomUUID(),
            1,
            "translation.key.pizza.margherita",
            null,
            new BigDecimal("100.00"),
            new BigDecimal("200.00"),
            1,
            BigDecimal.ZERO,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    when(itemRepository.findByIdAndPizzeriaId(pizzaId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(pizza));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(pizzaId))
        .thenReturn(Flux.empty());

    // User exists only in OTHER_PIZZERIA
    when(userRepository.findByIdAndPizzeriaId(userId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.empty());

    PizzaSuitabilityRequest request = new PizzaSuitabilityRequest(pizzaId, List.of(), List.of());

    // Suitability check should fail because user not found in DEFAULT_PIZZERIA
    StepVerifier.create(pizzaService.suitability(request, userId, DEFAULT_PIZZERIA_ID))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  @Test
  void suitabilityFailsWhenPizzaIsFromDifferentPizzeria() {
    // User exists in DEFAULT_PIZZERIA
    User user =
        User.builder()
            .id(userId)
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("Test User")
            .email("test@example.com")
            .preferredDiet(Diet.NONE)
            .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
            .updatedAt(Instant.parse("2024-01-01T00:00:00Z"))
            .build();

    when(userRepository.findByIdAndPizzeriaId(userId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(user));

    // Pizza does not exist in DEFAULT_PIZZERIA (it's in another pizzeria)
    when(itemRepository.findByIdAndPizzeriaId(pizzaId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.empty());
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(pizzaId))
        .thenReturn(Flux.empty());

    PizzaSuitabilityRequest request = new PizzaSuitabilityRequest(pizzaId, List.of(), List.of());

    // Suitability check should fail because pizza not found in DEFAULT_PIZZERIA
    StepVerifier.create(pizzaService.suitability(request, userId, DEFAULT_PIZZERIA_ID))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  @Test
  void listReturnsOnlyPizzasFromRequestedPizzeria() {
    UUID sectionId = UUID.randomUUID();
    UUID pizzaOneId = UUID.randomUUID();
    UUID pizzaTwoId = UUID.randomUUID();

    MenuSectionEntity section =
        new MenuSectionEntity(
            sectionId,
            DEFAULT_PIZZERIA_ID,
            "pizza-section",
            "translation.key.section.pizza",
            1,
            Instant.now(),
            Instant.now());

    MenuItemEntity pizzaOne =
        new MenuItemEntity(
            pizzaOneId,
            DEFAULT_PIZZERIA_ID,
            sectionId,
            1,
            "translation.key.pizza.one",
            null,
            new BigDecimal("100.00"),
            new BigDecimal("200.00"),
            1,
            BigDecimal.ZERO,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    MenuItemEntity pizzaTwo =
        new MenuItemEntity(
            pizzaTwoId,
            DEFAULT_PIZZERIA_ID,
            sectionId,
            2,
            "translation.key.pizza.two",
            null,
            new BigDecimal("110.00"),
            new BigDecimal("220.00"),
            2,
            BigDecimal.ZERO,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    // DEFAULT_PIZZERIA has sections and pizzas
    when(sectionRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.just(section));
    when(itemRepository.findAllBySectionIdOrderBySortOrderAsc(sectionId))
        .thenReturn(Flux.just(pizzaOne, pizzaTwo));

    // Mock ingredients for dietary type calculation
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(pizzaOneId))
        .thenReturn(Flux.empty());
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(pizzaTwoId))
        .thenReturn(Flux.empty());

    // OTHER_PIZZERIA has no sections
    when(sectionRepository.findAllByPizzeriaIdOrderBySortOrderAsc(OTHER_PIZZERIA_ID))
        .thenReturn(Flux.empty());

    // List from DEFAULT_PIZZERIA should return 2 pizzas
    StepVerifier.create(pizzaService.list(DEFAULT_PIZZERIA_ID).collectList())
        .assertNext(
            pizzas -> {
              assertThat(pizzas).hasSize(2);
              assertThat(pizzas.get(0).id()).isEqualTo(pizzaOneId);
              assertThat(pizzas.get(1).id()).isEqualTo(pizzaTwoId);
            })
        .verifyComplete();

    // List from OTHER_PIZZERIA should return empty
    StepVerifier.create(pizzaService.list(OTHER_PIZZERIA_ID).collectList())
        .assertNext(pizzas -> assertThat(pizzas).isEmpty())
        .verifyComplete();
  }

  @Test
  void priceForCustomisationsReturnsZeroWhenCustomisationsFromDifferentPizzeria() {
    UUID customisationId = UUID.randomUUID();

    // Customisation exists only in OTHER_PIZZERIA
    when(customisationRepository.findAllByPizzeriaIdOrderBySortOrderAsc(OTHER_PIZZERIA_ID))
        .thenReturn(
            Flux.just(
                new PizzaCustomisationEntity(
                    customisationId,
                    OTHER_PIZZERIA_ID,
                    "translation.key.customisation.extra.cheese",
                    new BigDecimal("25.00"),
                    null,
                    1,
                    Instant.parse("2024-01-01T00:00:00Z"),
                    Instant.parse("2024-01-01T00:00:00Z"))));

    // DEFAULT_PIZZERIA has no customisations
    when(customisationRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.empty());

    // Requesting price for customisation from OTHER_PIZZERIA while querying DEFAULT_PIZZERIA
    // should return zero (customisation not found in DEFAULT_PIZZERIA)
    StepVerifier.create(
            pizzaService.priceForCustomisations(
                List.of(customisationId), DEFAULT_PIZZERIA_ID, PizzaSize.REGULAR))
        .expectNext(BigDecimal.ZERO)
        .verifyComplete();
  }

  @Test
  void suitabilityIsolatesIngredientFactsBetweenPizzerias() {
    UUID ingredientFactId = UUID.randomUUID();

    // User exists in DEFAULT_PIZZERIA
    User user =
        User.builder()
            .id(userId)
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("Test User")
            .email("test@example.com")
            .preferredDiet(Diet.VEGAN)
            .preferredIngredientIds(Set.of(ingredientFactId))
            .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
            .updatedAt(Instant.parse("2024-01-01T00:00:00Z"))
            .build();

    MenuItemEntity pizza =
        new MenuItemEntity(
            pizzaId,
            DEFAULT_PIZZERIA_ID,
            UUID.randomUUID(),
            1,
            "translation.key.pizza.margherita",
            null,
            new BigDecimal("100.00"),
            new BigDecimal("200.00"),
            1,
            BigDecimal.ZERO,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-01T00:00:00Z"));

    when(userRepository.findByIdAndPizzeriaId(userId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(user));
    when(itemRepository.findByIdAndPizzeriaId(pizzaId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(pizza));
    when(ingredientRepository.findAllByMenuItemIdOrderBySortOrderAsc(pizzaId))
        .thenReturn(
            Flux.just(
                new MenuItemIngredientEntity(
                    UUID.randomUUID(), pizzaId, "translation.key.ingredient.tomato", 1)));

    // Ingredient facts are scoped to pizzeria
    when(ingredientFactRepository.findByIngredientKeyAndPizzeriaId(
            "translation.key.ingredient.tomato", DEFAULT_PIZZERIA_ID))
        .thenReturn(
            Mono.just(
                new MenuIngredientFactEntity(
                    UUID.randomUUID(),
                    DEFAULT_PIZZERIA_ID,
                    "translation.key.ingredient.tomato",
                    "VEGAN",
                    "",
                    0,
                    BigDecimal.ZERO)));

    // Preferred ingredient lookup is also scoped to pizzeria
    when(ingredientFactRepository.findAllByIdInAndPizzeriaId(
            org.mockito.ArgumentMatchers.<java.util.Collection<UUID>>any(),
            org.mockito.ArgumentMatchers.eq(DEFAULT_PIZZERIA_ID)))
        .thenReturn(Flux.empty());

    PizzaSuitabilityRequest request = new PizzaSuitabilityRequest(pizzaId, List.of(), List.of());

    StepVerifier.create(pizzaService.suitability(request, userId, DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              // Should be suitable since tomato is vegan
              assertThat(response.suitable()).isTrue();
              assertThat(response.violations()).isEmpty();
            })
        .verifyComplete();
  }

  @Test
  void priceForCustomisationsUsesFamilyPriceWhenFamilySizeAndPriceAvailable() {
    UUID customisationOne = UUID.randomUUID();
    UUID customisationTwo = UUID.randomUUID();

    when(customisationRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(
            Flux.just(
                new PizzaCustomisationEntity(
                    customisationOne,
                    DEFAULT_PIZZERIA_ID,
                    "translation.key.customisation.extra.cheese",
                    new BigDecimal("25.00"),
                    new BigDecimal("40.00"),
                    1,
                    Instant.parse("2024-01-01T00:00:00Z"),
                    Instant.parse("2024-01-01T00:00:00Z")),
                new PizzaCustomisationEntity(
                    customisationTwo,
                    DEFAULT_PIZZERIA_ID,
                    "translation.key.customisation.extra.pepperoni",
                    new BigDecimal("30.00"),
                    new BigDecimal("50.00"),
                    2,
                    Instant.parse("2024-01-01T00:00:00Z"),
                    Instant.parse("2024-01-01T00:00:00Z"))));

    StepVerifier.create(
            pizzaService.priceForCustomisations(
                List.of(customisationOne, customisationTwo), DEFAULT_PIZZERIA_ID, PizzaSize.FAMILY))
        .expectNext(new BigDecimal("90.00"))
        .verifyComplete();
  }

  @Test
  void priceForCustomisationsFallsBackToRegularWhenFamilyPriceNull() {
    UUID customisationOne = UUID.randomUUID();
    UUID customisationTwo = UUID.randomUUID();

    when(customisationRepository.findAllByPizzeriaIdOrderBySortOrderAsc(DEFAULT_PIZZERIA_ID))
        .thenReturn(
            Flux.just(
                new PizzaCustomisationEntity(
                    customisationOne,
                    DEFAULT_PIZZERIA_ID,
                    "translation.key.customisation.extra.cheese",
                    new BigDecimal("25.00"),
                    new BigDecimal("40.00"),
                    1,
                    Instant.parse("2024-01-01T00:00:00Z"),
                    Instant.parse("2024-01-01T00:00:00Z")),
                new PizzaCustomisationEntity(
                    customisationTwo,
                    DEFAULT_PIZZERIA_ID,
                    "translation.key.customisation.gluten.free",
                    new BigDecimal("30.00"),
                    null,
                    2,
                    Instant.parse("2024-01-01T00:00:00Z"),
                    Instant.parse("2024-01-01T00:00:00Z"))));

    // customisationOne has family price (40), customisationTwo falls back to regular (30)
    StepVerifier.create(
            pizzaService.priceForCustomisations(
                List.of(customisationOne, customisationTwo), DEFAULT_PIZZERIA_ID, PizzaSize.FAMILY))
        .expectNext(new BigDecimal("70.00"))
        .verifyComplete();
  }
}
