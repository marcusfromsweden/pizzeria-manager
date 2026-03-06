package com.pizzeriaservice.service.menu;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MenuItemEntityTest {

  @Test
  void shouldCreateMenuItemWithCalories() {
    UUID id = UUID.randomUUID();
    UUID pizzeriaId = UUID.randomUUID();
    UUID sectionId = UUID.randomUUID();
    BigDecimal calories = BigDecimal.valueOf(850.50);
    Instant now = Instant.now();

    MenuItemEntity entity =
        new MenuItemEntity(
            id,
            pizzeriaId,
            sectionId,
            1,
            "pizza.margarita",
            "desc.pizza.margarita",
            BigDecimal.valueOf(89.00),
            BigDecimal.valueOf(129.00),
            1,
            calories,
            now,
            now);

    assertThat(entity.id()).isEqualTo(id);
    assertThat(entity.totalCalories()).isEqualTo(calories);
    assertThat(entity.totalCalories()).isGreaterThan(BigDecimal.ZERO);
  }

  @Test
  void shouldHandleZeroCalories() {
    UUID id = UUID.randomUUID();
    UUID pizzeriaId = UUID.randomUUID();
    UUID sectionId = UUID.randomUUID();
    Instant now = Instant.now();

    MenuItemEntity entity =
        new MenuItemEntity(
            id,
            pizzeriaId,
            sectionId,
            1,
            "test.item",
            "test.desc",
            BigDecimal.valueOf(50.00),
            null,
            1,
            BigDecimal.ZERO,
            now,
            now);

    assertThat(entity.totalCalories()).isEqualTo(BigDecimal.ZERO);
  }
}
