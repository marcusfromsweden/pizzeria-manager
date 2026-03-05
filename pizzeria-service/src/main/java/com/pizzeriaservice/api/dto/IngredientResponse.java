package com.pizzeriaservice.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record IngredientResponse(
    UUID id,
    String name,
    String category,
    String originRegion,
    String dietaryType,
    Set<String> allergenTags,
    int spiceLevel,
    String availability,
    BigDecimal unitPrice,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {}
