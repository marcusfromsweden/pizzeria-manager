package com.pizzeriaservice.service.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class AuthTokenServiceTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final String TEST_JWT_SECRET = "test-secret-key-minimum-32-characters-long";
  private static final long TEST_EXPIRATION_MS = 3600000; // 1 hour

  private AuthTokenService authTokenService;

  @BeforeEach
  void setUp() {
    authTokenService = new AuthTokenService(TEST_JWT_SECRET, TEST_EXPIRATION_MS);
  }

  @Test
  void generateTokenStoresUserAndPizzeriaMapping() {
    UUID userId = UUID.randomUUID();
    UUID pizzeriaId = DEFAULT_PIZZERIA_ID;

    StepVerifier.create(authTokenService.generateToken(userId, pizzeriaId, null))
        .assertNext(
            token ->
                StepVerifier.create(authTokenService.resolveUser(token))
                    .assertNext(
                        authenticatedUser -> {
                          assertThat(authenticatedUser.userId()).isEqualTo(userId);
                          assertThat(authenticatedUser.pizzeriaId()).isEqualTo(pizzeriaId);
                        })
                    .verifyComplete())
        .verifyComplete();
  }

  @Test
  void generateTokenIncludesPizzeriaAdmin() {
    UUID userId = UUID.randomUUID();
    UUID pizzeriaId = DEFAULT_PIZZERIA_ID;
    String pizzeriaAdmin = "testpizzeria";

    StepVerifier.create(authTokenService.generateToken(userId, pizzeriaId, pizzeriaAdmin))
        .assertNext(
            token ->
                StepVerifier.create(authTokenService.resolveUser(token))
                    .assertNext(
                        authenticatedUser -> {
                          assertThat(authenticatedUser.userId()).isEqualTo(userId);
                          assertThat(authenticatedUser.pizzeriaId()).isEqualTo(pizzeriaId);
                          assertThat(authenticatedUser.pizzeriaAdmin()).isEqualTo(pizzeriaAdmin);
                        })
                    .verifyComplete())
        .verifyComplete();
  }

  @Test
  void resolveUserReturnsEmptyForUnknownToken() {
    StepVerifier.create(authTokenService.resolveUser("invalid-token")).verifyComplete();
  }

  @Test
  void resolveUserReturnsEmptyForMalformedToken() {
    StepVerifier.create(authTokenService.resolveUser("not.a.valid.jwt")).verifyComplete();
  }

  @Test
  void invalidateTokenIsNoOpForJwt() {
    // JWT tokens are stateless, so invalidation doesn't prevent reuse
    // This test documents the expected behavior
    UUID userId = UUID.randomUUID();
    UUID pizzeriaId = DEFAULT_PIZZERIA_ID;

    StepVerifier.create(authTokenService.generateToken(userId, pizzeriaId, null))
        .assertNext(
            token -> {
              StepVerifier.create(authTokenService.invalidateToken(token)).verifyComplete();
              // Token is still valid after "invalidation" because JWT is stateless
              StepVerifier.create(authTokenService.resolveUser(token))
                  .assertNext(
                      authenticatedUser -> {
                        assertThat(authenticatedUser.userId()).isEqualTo(userId);
                      })
                  .verifyComplete();
            })
        .verifyComplete();
  }

  @Test
  void expiredTokenReturnsEmpty() {
    // Create service with very short expiration
    AuthTokenService shortExpirationService = new AuthTokenService(TEST_JWT_SECRET, 1);
    UUID userId = UUID.randomUUID();

    String token = shortExpirationService.generateToken(userId, DEFAULT_PIZZERIA_ID, null).block();

    // Wait for token to expire
    try {
      Thread.sleep(50);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    StepVerifier.create(shortExpirationService.resolveUser(token)).verifyComplete();
  }

  @Test
  void tokenSignedWithDifferentSecretIsRejected() {
    AuthTokenService otherService =
        new AuthTokenService("different-secret-key-minimum-32-chars", TEST_EXPIRATION_MS);

    String tokenFromOtherService =
        otherService.generateToken(UUID.randomUUID(), DEFAULT_PIZZERIA_ID, null).block();

    // Token from other service should not be valid in our service
    StepVerifier.create(authTokenService.resolveUser(tokenFromOtherService)).verifyComplete();
  }
}
