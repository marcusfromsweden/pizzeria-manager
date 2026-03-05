package com.pizzeriaservice.service.support.security;

import java.util.UUID;

/**
 * Represents the authenticated user context containing both user and pizzeria identifiers. This
 * record is used as the principal in Spring Security's authentication context.
 */
public record AuthenticatedUser(UUID userId, UUID pizzeriaId, String pizzeriaAdmin) {

  public AuthenticatedUser {
    if (userId == null) {
      throw new IllegalArgumentException("userId cannot be null");
    }
    if (pizzeriaId == null) {
      throw new IllegalArgumentException("pizzeriaId cannot be null");
    }
    // pizzeriaAdmin can be null (user is not an admin)
  }

  /**
   * Check if this user is an admin for the given pizzeria code.
   *
   * @param pizzeriaCode the pizzeria code to check
   * @return true if user is admin for this pizzeria
   */
  public boolean isAdminFor(String pizzeriaCode) {
    return pizzeriaAdmin != null && pizzeriaAdmin.equals(pizzeriaCode);
  }
}
