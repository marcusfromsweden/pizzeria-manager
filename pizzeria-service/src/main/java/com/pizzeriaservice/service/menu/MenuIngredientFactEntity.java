package com.pizzeriaservice.service.menu;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("menu_ingredient_facts")
public record MenuIngredientFactEntity(
    @Id UUID id,
    @Column("pizzeria_id") UUID pizzeriaId,
    @Column("ingredient_key") String ingredientKey,
    @Column("dietary_type") String dietaryType,
    @Column("allergen_tags") String allergenTags,
    @Column("spice_level") Integer spiceLevel) {}
