package com.pizzeriaservice.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pizzeriaservice.api.dto.CreateOrderRequest;
import com.pizzeriaservice.api.dto.OrderItemRequest;
import com.pizzeriaservice.service.menu.MenuItemEntity;
import com.pizzeriaservice.service.menu.PizzaCustomisationEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderValidatorTest {

  private static final UUID PIZZERIA_ID = UUID.randomUUID();
  private static final UUID SECTION_ID = UUID.randomUUID();
  private static final Instant NOW = Instant.now();

  private OrderValidator validator;

  @BeforeEach
  void setUp() {
    validator = new OrderValidator();
  }

  @Test
  void shouldPassWhenAllItemsAreValid() {
    UUID menuItemId = UUID.randomUUID();
    MenuItemEntity menuItem =
        new MenuItemEntity(
            menuItemId,
            PIZZERIA_ID,
            SECTION_ID,
            1,
            "pizza.margherita",
            "pizza.margherita.desc",
            new BigDecimal("99.00"),
            new BigDecimal("149.00"),
            1,
            BigDecimal.ZERO,
            NOW,
            NOW);

    UUID custId = UUID.randomUUID();
    PizzaCustomisationEntity customisation =
        new PizzaCustomisationEntity(
            custId,
            PIZZERIA_ID,
            "extra.cheese",
            new BigDecimal("15.00"),
            new BigDecimal("25.00"),
            1,
            NOW,
            NOW);

    OrderItemRequest item = new OrderItemRequest(menuItemId, "REGULAR", 1, List.of(custId), null);
    CreateOrderRequest request =
        new CreateOrderRequest(
            "PICKUP", null, null, null, null, null, null, null, null, List.of(item));

    assertThatCode(
            () ->
                validator.validateOrderItems(
                    request, Map.of(menuItemId, menuItem), Map.of(custId, customisation)))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectMissingMenuItem() {
    UUID missingMenuItemId = UUID.randomUUID();

    OrderItemRequest item = new OrderItemRequest(missingMenuItemId, "REGULAR", 1, null, null);
    CreateOrderRequest request =
        new CreateOrderRequest(
            "PICKUP", null, null, null, null, null, null, null, null, List.of(item));

    assertThatThrownBy(() -> validator.validateOrderItems(request, Map.of(), Map.of()))
        .isInstanceOf(DomainValidationException.class)
        .satisfies(
            ex -> {
              DomainValidationException dve = (DomainValidationException) ex;
              assertThat(dve.getViolations())
                  .containsExactly("Menu item not found: " + missingMenuItemId);
            });
  }

  @Test
  void shouldRejectFamilySizeWhenNotAvailable() {
    UUID menuItemId = UUID.randomUUID();
    MenuItemEntity menuItem =
        new MenuItemEntity(
            menuItemId,
            PIZZERIA_ID,
            SECTION_ID,
            1,
            "pizza.margherita",
            "pizza.margherita.desc",
            new BigDecimal("99.00"),
            null,
            1,
            BigDecimal.ZERO,
            NOW,
            NOW);

    OrderItemRequest item = new OrderItemRequest(menuItemId, "FAMILY", 1, null, null);
    CreateOrderRequest request =
        new CreateOrderRequest(
            "PICKUP", null, null, null, null, null, null, null, null, List.of(item));

    assertThatThrownBy(
            () -> validator.validateOrderItems(request, Map.of(menuItemId, menuItem), Map.of()))
        .isInstanceOf(DomainValidationException.class)
        .satisfies(
            ex -> {
              DomainValidationException dve = (DomainValidationException) ex;
              assertThat(dve.getViolations())
                  .containsExactly("Family size is not available for menu item: pizza.margherita");
            });
  }

  @Test
  void shouldRejectMissingCustomisation() {
    UUID menuItemId = UUID.randomUUID();
    MenuItemEntity menuItem =
        new MenuItemEntity(
            menuItemId,
            PIZZERIA_ID,
            SECTION_ID,
            1,
            "pizza.margherita",
            "pizza.margherita.desc",
            new BigDecimal("99.00"),
            new BigDecimal("149.00"),
            1,
            BigDecimal.ZERO,
            NOW,
            NOW);

    UUID missingCustId = UUID.randomUUID();
    OrderItemRequest item =
        new OrderItemRequest(menuItemId, "REGULAR", 1, List.of(missingCustId), null);
    CreateOrderRequest request =
        new CreateOrderRequest(
            "PICKUP", null, null, null, null, null, null, null, null, List.of(item));

    assertThatThrownBy(
            () -> validator.validateOrderItems(request, Map.of(menuItemId, menuItem), Map.of()))
        .isInstanceOf(DomainValidationException.class)
        .satisfies(
            ex -> {
              DomainValidationException dve = (DomainValidationException) ex;
              assertThat(dve.getViolations())
                  .containsExactly("Customisation not found: " + missingCustId);
            });
  }

  @Test
  void shouldRejectFamilySizeCustomisationWhenPriceNotAvailable() {
    UUID menuItemId = UUID.randomUUID();
    MenuItemEntity menuItem =
        new MenuItemEntity(
            menuItemId,
            PIZZERIA_ID,
            SECTION_ID,
            1,
            "pizza.margherita",
            "pizza.margherita.desc",
            new BigDecimal("99.00"),
            new BigDecimal("149.00"),
            1,
            BigDecimal.ZERO,
            NOW,
            NOW);

    UUID custId = UUID.randomUUID();
    PizzaCustomisationEntity customisation =
        new PizzaCustomisationEntity(
            custId, PIZZERIA_ID, "extra.cheese", new BigDecimal("15.00"), null, 1, NOW, NOW);

    OrderItemRequest item = new OrderItemRequest(menuItemId, "FAMILY", 1, List.of(custId), null);
    CreateOrderRequest request =
        new CreateOrderRequest(
            "PICKUP", null, null, null, null, null, null, null, null, List.of(item));

    assertThatThrownBy(
            () ->
                validator.validateOrderItems(
                    request, Map.of(menuItemId, menuItem), Map.of(custId, customisation)))
        .isInstanceOf(DomainValidationException.class)
        .satisfies(
            ex -> {
              DomainValidationException dve = (DomainValidationException) ex;
              assertThat(dve.getViolations())
                  .containsExactly(
                      "Family size price is not available for customisation: extra.cheese");
            });
  }

  @Test
  void shouldCollectMultipleViolations() {
    UUID missingMenuItemId = UUID.randomUUID();
    UUID menuItemId = UUID.randomUUID();
    MenuItemEntity menuItem =
        new MenuItemEntity(
            menuItemId,
            PIZZERIA_ID,
            SECTION_ID,
            1,
            "pizza.hawaii",
            "pizza.hawaii.desc",
            new BigDecimal("109.00"),
            null,
            2,
            BigDecimal.ZERO,
            NOW,
            NOW);

    UUID missingCustId = UUID.randomUUID();

    OrderItemRequest missingItem =
        new OrderItemRequest(missingMenuItemId, "REGULAR", 1, null, null);
    OrderItemRequest familyItemWithMissingCust =
        new OrderItemRequest(menuItemId, "FAMILY", 2, List.of(missingCustId), null);

    CreateOrderRequest request =
        new CreateOrderRequest(
            "PICKUP",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of(missingItem, familyItemWithMissingCust));

    assertThatThrownBy(
            () -> validator.validateOrderItems(request, Map.of(menuItemId, menuItem), Map.of()))
        .isInstanceOf(DomainValidationException.class)
        .satisfies(
            ex -> {
              DomainValidationException dve = (DomainValidationException) ex;
              assertThat(dve.getViolations())
                  .hasSize(3)
                  .containsExactly(
                      "Menu item not found: " + missingMenuItemId,
                      "Family size is not available for menu item: pizza.hawaii",
                      "Customisation not found: " + missingCustId);
            });
  }

  @Test
  void shouldPassWithNullCustomisationIds() {
    UUID menuItemId = UUID.randomUUID();
    MenuItemEntity menuItem =
        new MenuItemEntity(
            menuItemId,
            PIZZERIA_ID,
            SECTION_ID,
            1,
            "pizza.margherita",
            "pizza.margherita.desc",
            new BigDecimal("99.00"),
            new BigDecimal("149.00"),
            1,
            BigDecimal.ZERO,
            NOW,
            NOW);

    OrderItemRequest item = new OrderItemRequest(menuItemId, "REGULAR", 1, null, null);
    CreateOrderRequest request =
        new CreateOrderRequest(
            "PICKUP", null, null, null, null, null, null, null, null, List.of(item));

    assertThatCode(
            () -> validator.validateOrderItems(request, Map.of(menuItemId, menuItem), Map.of()))
        .doesNotThrowAnyException();
  }
}
