package com.pizzeriaservice.service.domain.model;

import com.pizzeriaservice.service.domain.Diet;
import com.pizzeriaservice.service.domain.UserStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record User(
    UUID id,
    UUID pizzeriaId,
    String name,
    String email,
    String passwordHash,
    boolean emailVerified,
    String phone,
    Diet preferredDiet,
    Set<UUID> preferredIngredientIds,
    UserStatus status,
    String pizzeriaAdmin,
    String profilePhotoBase64,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy,
    boolean isNew) {
  public User {
    if (preferredIngredientIds == null) {
      preferredIngredientIds = Set.of();
    }
    if (preferredDiet == null) {
      preferredDiet = Diet.NONE;
    }
  }
}
