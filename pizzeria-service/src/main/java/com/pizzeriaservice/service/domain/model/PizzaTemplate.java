package com.pizzeriaservice.service.domain.model;

import com.pizzeriaservice.service.domain.PizzaAvailabilityStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record PizzaTemplate(
    UUID id,
    String name,
    String description,
    BigDecimal basePrice,
    List<IngredientPortion> ingredients,
    Set<String> tags,
    PizzaAvailabilityStatus availabilityStatus,
    boolean published,
    int sortOrder,
    Instant createdAt,
    Instant updatedAt) {}
