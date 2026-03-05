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
public class VerificationTokenService {

  public record VerificationTokenData(UUID userId, UUID pizzeriaId) {
    public VerificationTokenData {
      if (userId == null) {
        throw new IllegalArgumentException("userId cannot be null");
      }
      if (pizzeriaId == null) {
        throw new IllegalArgumentException("pizzeriaId cannot be null");
      }
    }
  }

  private final SecureRandom secureRandom = new SecureRandom();
  private final Map<String, VerificationTokenData> tokens = new ConcurrentHashMap<>();

  public Mono<String> createToken(UUID userId, UUID pizzeriaId) {
    return Mono.fromSupplier(
        () -> {
          byte[] bytes = new byte[24];
          secureRandom.nextBytes(bytes);
          String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
          tokens.put(token, new VerificationTokenData(userId, pizzeriaId));
          return token;
        });
  }

  public Mono<VerificationTokenData> consumeToken(String token) {
    return Mono.justOrEmpty(Optional.ofNullable(tokens.remove(token)));
  }

  public Mono<VerificationTokenData> peekTokenData(String token) {
    return Mono.justOrEmpty(Optional.ofNullable(tokens.get(token)));
  }
}
