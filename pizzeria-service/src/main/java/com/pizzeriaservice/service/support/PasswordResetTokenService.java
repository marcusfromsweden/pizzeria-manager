package com.pizzeriaservice.service.support;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PasswordResetTokenService {

  public record PasswordResetTokenData(UUID userId, UUID pizzeriaId) {
    public PasswordResetTokenData {
      if (userId == null) {
        throw new IllegalArgumentException("userId cannot be null");
      }
      if (pizzeriaId == null) {
        throw new IllegalArgumentException("pizzeriaId cannot be null");
      }
    }
  }

  private final SecureRandom secureRandom = new SecureRandom();
  private final Map<String, PasswordResetTokenData> tokens = new ConcurrentHashMap<>();

  public Mono<String> createToken(UUID userId, UUID pizzeriaId) {
    return Mono.fromSupplier(
        () -> {
          byte[] bytes = new byte[24];
          secureRandom.nextBytes(bytes);
          String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
          tokens.put(token, new PasswordResetTokenData(userId, pizzeriaId));
          return token;
        });
  }

  public Mono<PasswordResetTokenData> consumeToken(String token) {
    return Mono.justOrEmpty(Optional.ofNullable(tokens.remove(token)));
  }
}
