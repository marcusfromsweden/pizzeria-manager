package com.pizzeriaservice.api.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String name,
    String email,
    boolean emailVerified,
    DietType preferredDiet,
    Set<UUID> preferredIngredientIds,
    String pizzeriaAdmin,
    String profilePhotoBase64,
    Instant createdAt,
    Instant updatedAt) {}
