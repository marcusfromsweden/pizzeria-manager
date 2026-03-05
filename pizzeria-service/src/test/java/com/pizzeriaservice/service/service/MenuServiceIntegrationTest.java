package com.pizzeriaservice.service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pizzeriaservice.api.dto.MenuResponse;
import com.pizzeriaservice.api.dto.MenuSectionResponse;
import com.pizzeriaservice.service.test.PizzeriaIntegrationTest;
import com.pizzeriaservice.service.test.PostgresContainerSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

@PizzeriaIntegrationTest
class MenuServiceIntegrationTest extends PostgresContainerSupport {

  // Menu data migrated to Ramona pizzeria
  private static final UUID RAMONA_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Autowired private MenuService menuService;

  @Test
  void liquibaseLoadsMenuDataFromJson() {
    StepVerifier.create(menuService.getMenu(RAMONA_PIZZERIA_ID))
        .assertNext(this::verifyMenu)
        .verifyComplete();
  }

  private void verifyMenu(MenuResponse response) {
    assertThat(response.sections()).isNotEmpty();
    MenuSectionResponse pizzas =
        response.sections().stream()
            .filter(section -> section.code().equals("pizzas"))
            .findFirst()
            .orElseThrow();

    assertThat(pizzas.items()).isNotEmpty();
    var margherita =
        pizzas.items().stream().filter(item -> item.dishNumber() == 1).findFirst().orElseThrow();

    assertThat(margherita.nameKey()).isEqualTo("translation.key.disc.pizza.margarita");
    assertThat(margherita.priceInSek()).isNotNull();
    assertThat(margherita.familySizePriceInSek()).isNotNull();
    assertThat(margherita.ingredients())
        .anySatisfy(
            ingredient -> {
              assertThat(ingredient.id()).isNotNull();
              assertThat(ingredient.ingredientKey()).isEqualTo("translation.key.ingredient.cheese");
            });

    assertThat(response.pizzaCustomisations()).isNotEmpty();
  }
}
