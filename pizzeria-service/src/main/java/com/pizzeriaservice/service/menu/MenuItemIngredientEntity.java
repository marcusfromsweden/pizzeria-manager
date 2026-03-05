package com.pizzeriaservice.service.menu;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("menu_item_ingredients")
public record MenuItemIngredientEntity(
    @Id UUID id,
    @Column("menu_item_id") UUID menuItemId,
    @Column("ingredient_key") String ingredientKey,
    @Column("sort_order") Integer sortOrder) {}
