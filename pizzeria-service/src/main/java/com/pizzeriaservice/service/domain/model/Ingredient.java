package com.pizzeriaservice.service.domain.model;

import com.pizzeriaservice.service.domain.DietaryType;
import com.pizzeriaservice.service.domain.IngredientAvailability;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Ingredient(
    UUID id,
    String name,
    String category,
    String originRegion,
    DietaryType dietaryType,
    Set<String> allergenTags,
    int spiceLevel,
    IngredientAvailability availability,
    BigDecimal unitPrice,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {}
