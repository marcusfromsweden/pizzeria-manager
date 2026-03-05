package com.pizzeriaservice.service.support;

import com.pizzeriaservice.service.support.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuthTokenService {

  private static final Logger log = LoggerFactory.getLogger(AuthTokenService.class);

  private static final String CLAIM_USER_ID = "userId";
  private static final String CLAIM_PIZZERIA_ID = "pizzeriaId";
  private static final String CLAIM_PIZZERIA_ADMIN = "pizzeriaAdmin";

  private final SecretKey secretKey;
  private final long expirationMs;

  public AuthTokenService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
    // Ensure the secret is at least 256 bits (32 bytes) for HS256
    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < 32) {
      throw new IllegalArgumentException("JWT secret must be at least 32 characters long");
    }
    this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    this.expirationMs = expirationMs;
  }

  public Mono<String> generateToken(UUID userId, UUID pizzeriaId, String pizzeriaAdmin) {
    return Mono.fromSupplier(
        () -> {
          Date now = new Date();
          Date expiration = new Date(now.getTime() + expirationMs);

          var builder =
              Jwts.builder()
                  .subject(userId.toString())
                  .claim(CLAIM_USER_ID, userId.toString())
                  .claim(CLAIM_PIZZERIA_ID, pizzeriaId.toString())
                  .issuedAt(now)
                  .expiration(expiration);

          Optional.ofNullable(pizzeriaAdmin)
              .ifPresent(admin -> builder.claim(CLAIM_PIZZERIA_ADMIN, admin));

          return builder.signWith(secretKey).compact();
        });
  }

  public Mono<AuthenticatedUser> resolveUser(String token) {
    return Mono.fromSupplier(() -> parseToken(token)).flatMap(Mono::justOrEmpty);
  }

  public Mono<Void> invalidateToken(String token) {
    // JWT tokens are stateless, so we don't need to invalidate them server-side.
    // The token will simply expire after its expiration time.
    // For enhanced security, you could maintain a blacklist of revoked tokens,
    // but for simplicity we just return success here.
    log.debug("Token invalidation requested (JWT is stateless, no server-side invalidation)");
    return Mono.empty();
  }

  private Optional<AuthenticatedUser> parseToken(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();

      UUID userId = UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
      UUID pizzeriaId = UUID.fromString(claims.get(CLAIM_PIZZERIA_ID, String.class));
      String pizzeriaAdmin = claims.get(CLAIM_PIZZERIA_ADMIN, String.class);

      return Optional.of(new AuthenticatedUser(userId, pizzeriaId, pizzeriaAdmin));
    } catch (ExpiredJwtException e) {
      log.debug("JWT token expired: {}", e.getMessage());
      return Optional.empty();
    } catch (JwtException e) {
      log.debug("Invalid JWT token: {}", e.getMessage());
      return Optional.empty();
    } catch (IllegalArgumentException e) {
      log.debug("Failed to parse JWT claims: {}", e.getMessage());
      return Optional.empty();
    }
  }
}
