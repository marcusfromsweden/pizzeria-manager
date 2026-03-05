package com.pizzeriaservice.service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.api.dto.MenuItemResponse;
import com.pizzeriaservice.api.dto.MenuResponse;
import com.pizzeriaservice.api.dto.MenuSectionResponse;
import com.pizzeriaservice.api.dto.PizzaDetailResponse;
import com.pizzeriaservice.api.dto.PizzaSuitabilityRequest;
import com.pizzeriaservice.api.dto.PizzaSuitabilityResponse;
import com.pizzeriaservice.service.service.MenuService;
import com.pizzeriaservice.service.service.PizzaService;
import com.pizzeriaservice.service.service.PizzeriaService;
import com.pizzeriaservice.service.support.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PizzaControllerTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final String PIZZERIA_CODE = "testpizzeria";

  @Mock private PizzaService pizzaService;
  @Mock private MenuService menuService;
  @Mock private PizzeriaService pizzeriaService;

  private PizzaController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new PizzaController(pizzaService, menuService, pizzeriaService);
  }

  @Test
  void listResolvesPizzeriaAndReturnsPizzasFromPizzaSections() {
    UUID pizzaId1 = UUID.randomUUID();
    UUID pizzaId2 = UUID.randomUUID();
    UUID saladId = UUID.randomUUID();

    MenuItemResponse pizzaItem1 =
        new MenuItemResponse(
            pizzaId1,
            UUID.randomUUID(),
            1,
            "pizza.margherita",
            "pizza.margherita.desc",
            BigDecimal.valueOf(95),
            BigDecimal.valueOf(165),
            List.of(),
            "VEGETARIAN",
            1);
    MenuItemResponse pizzaItem2 =
        new MenuItemResponse(
            pizzaId2,
            UUID.randomUUID(),
            2,
            "pizza.pepperoni",
            "pizza.pepperoni.desc",
            BigDecimal.valueOf(105),
            BigDecimal.valueOf(175),
            List.of(),
            "CARNIVORE",
            2);
    MenuItemResponse saladItem =
        new MenuItemResponse(
            saladId,
            UUID.randomUUID(),
            101,
            "salad.caesar",
            "salad.caesar.desc",
            BigDecimal.valueOf(75),
            null,
            List.of(),
            "VEGAN",
            1);

    MenuSectionResponse pizzaSection =
        new MenuSectionResponse(
            UUID.randomUUID(), "pizza", "menu.section.pizza", 1, List.of(pizzaItem1, pizzaItem2));
    MenuSectionResponse saladSection =
        new MenuSectionResponse(
            UUID.randomUUID(), "salads", "menu.section.salads", 2, List.of(saladItem));

    MenuResponse menuResponse = new MenuResponse(List.of(pizzaSection, saladSection), List.of());

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(menuService.getMenu(DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(menuResponse));

    StepVerifier.create(controller.list(PIZZERIA_CODE).collectList())
        .assertNext(
            summaries -> {
              assertThat(summaries).hasSize(2);
              assertThat(summaries.get(0).id()).isEqualTo(pizzaId1);
              assertThat(summaries.get(0).nameKey()).isEqualTo("pizza.margherita");
              assertThat(summaries.get(1).id()).isEqualTo(pizzaId2);
              assertThat(summaries.get(1).nameKey()).isEqualTo("pizza.pepperoni");
            })
        .verifyComplete();

    verify(pizzeriaService).resolvePizzeriaId(PIZZERIA_CODE);
    verify(menuService).getMenu(DEFAULT_PIZZERIA_ID);
  }

  @Test
  void listReturnsEmptyFluxWhenNoPizzaSections() {
    MenuSectionResponse saladSection =
        new MenuSectionResponse(UUID.randomUUID(), "salads", "menu.section.salads", 1, List.of());

    MenuResponse menuResponse = new MenuResponse(List.of(saladSection), List.of());

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(menuService.getMenu(DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(menuResponse));

    StepVerifier.create(controller.list(PIZZERIA_CODE).collectList())
        .assertNext(summaries -> assertThat(summaries).isEmpty())
        .verifyComplete();
  }

  @Test
  void getResolvesPizzeriaAndDelegatesToService() {
    UUID pizzaId = UUID.randomUUID();
    PizzaDetailResponse response =
        new PizzaDetailResponse(
            pizzaId,
            1,
            "pizza.margherita",
            "pizza.margherita.desc",
            BigDecimal.valueOf(95),
            BigDecimal.valueOf(165),
            List.of(),
            "VEGETARIAN",
            1);

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(pizzaService.get(pizzaId, DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(response));

    StepVerifier.create(controller.get(PIZZERIA_CODE, pizzaId))
        .expectNext(response)
        .verifyComplete();

    verify(pizzeriaService).resolvePizzeriaId(PIZZERIA_CODE);
    verify(pizzaService).get(pizzaId, DEFAULT_PIZZERIA_ID);
  }

  @Test
  void suitabilityDelegatesToServiceWithAuthenticatedUser() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);
    UUID pizzaId = UUID.randomUUID();
    PizzaSuitabilityRequest request = new PizzaSuitabilityRequest(pizzaId, List.of(), List.of());
    PizzaSuitabilityResponse response =
        new PizzaSuitabilityResponse(true, List.of(), List.of("Add preferred ingredient cheese"));

    when(pizzaService.suitability(request, userId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(response));

    StepVerifier.create(controller.suitability(user, request))
        .expectNext(response)
        .verifyComplete();

    verify(pizzaService).suitability(request, userId, DEFAULT_PIZZERIA_ID);
  }

  @Test
  void suitabilityReturnsViolationsWhenNotSuitable() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);
    UUID pizzaId = UUID.randomUUID();
    PizzaSuitabilityRequest request = new PizzaSuitabilityRequest(pizzaId, List.of(), List.of());
    PizzaSuitabilityResponse response =
        new PizzaSuitabilityResponse(
            false, List.of("Ingredient meat violates vegan diet"), List.of());

    when(pizzaService.suitability(request, userId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(response));

    StepVerifier.create(controller.suitability(user, request))
        .assertNext(
            result -> {
              assertThat(result.suitable()).isFalse();
              assertThat(result.violations())
                  .containsExactly("Ingredient meat violates vegan diet");
            })
        .verifyComplete();
  }
}
