package com.pizzeriaservice.service.support.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.pizzeriaservice.service.support.AuthTokenService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import reactor.test.StepVerifier;

class AuthTokenAuthenticationManagerTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID OTHER_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final String TEST_JWT_SECRET = "test-secret-key-minimum-32-characters-long";
  private static final long TEST_EXPIRATION_MS = 3600000; // 1 hour

  private AuthTokenService authTokenService;
  private AuthTokenAuthenticationManager authenticationManager;

  @BeforeEach
  void setUp() {
    authTokenService = new AuthTokenService(TEST_JWT_SECRET, TEST_EXPIRATION_MS);
    authenticationManager = new AuthTokenAuthenticationManager(authTokenService);
  }

  @Test
  void authenticateReturnsAuthenticatedTokenForValidToken() {
    UUID userId = UUID.randomUUID();

    // Generate a valid token
    String token = authTokenService.generateToken(userId, DEFAULT_PIZZERIA_ID, null).block();

    // Create unauthenticated token
    AuthTokenAuthentication unauthenticated = AuthTokenAuthentication.unauthenticated(token);

    StepVerifier.create(authenticationManager.authenticate(unauthenticated))
        .assertNext(
            auth -> {
              assertThat(auth).isInstanceOf(AuthTokenAuthentication.class);
              AuthTokenAuthentication authToken = (AuthTokenAuthentication) auth;
              assertThat(authToken.isAuthenticated()).isTrue();
              assertThat(authToken.getUserId()).isEqualTo(userId);
              assertThat(authToken.getPizzeriaId()).isEqualTo(DEFAULT_PIZZERIA_ID);
              assertThat(authToken.getCredentials()).isEqualTo(token);
            })
        .verifyComplete();
  }

  @Test
  void authenticateReturnsEmptyForInvalidToken() {
    // Create unauthenticated token with invalid token string
    AuthTokenAuthentication unauthenticated =
        AuthTokenAuthentication.unauthenticated("invalid-token");

    StepVerifier.create(authenticationManager.authenticate(unauthenticated)).verifyComplete();
  }

  @Test
  void authenticateReturnsEmptyForNonAuthTokenAuthentication() {
    // Use a different authentication type
    UsernamePasswordAuthenticationToken usernamePassword =
        new UsernamePasswordAuthenticationToken("user", "password");

    StepVerifier.create(authenticationManager.authenticate(usernamePassword)).verifyComplete();
  }

  @Test
  void authenticatePreservesPizzeriaIdFromToken() {
    UUID userOneId = UUID.randomUUID();
    UUID userTwoId = UUID.randomUUID();

    // Generate tokens for different pizzerias
    String tokenOne = authTokenService.generateToken(userOneId, DEFAULT_PIZZERIA_ID, null).block();
    String tokenTwo = authTokenService.generateToken(userTwoId, OTHER_PIZZERIA_ID, null).block();

    // Authenticate first token
    AuthTokenAuthentication unauthenticatedOne = AuthTokenAuthentication.unauthenticated(tokenOne);
    StepVerifier.create(authenticationManager.authenticate(unauthenticatedOne))
        .assertNext(
            auth -> {
              AuthTokenAuthentication authToken = (AuthTokenAuthentication) auth;
              assertThat(authToken.getUserId()).isEqualTo(userOneId);
              assertThat(authToken.getPizzeriaId()).isEqualTo(DEFAULT_PIZZERIA_ID);
            })
        .verifyComplete();

    // Authenticate second token
    AuthTokenAuthentication unauthenticatedTwo = AuthTokenAuthentication.unauthenticated(tokenTwo);
    StepVerifier.create(authenticationManager.authenticate(unauthenticatedTwo))
        .assertNext(
            auth -> {
              AuthTokenAuthentication authToken = (AuthTokenAuthentication) auth;
              assertThat(authToken.getUserId()).isEqualTo(userTwoId);
              assertThat(authToken.getPizzeriaId()).isEqualTo(OTHER_PIZZERIA_ID);
            })
        .verifyComplete();
  }

  @Test
  void authenticateStillWorksAfterInvalidateTokenForJwt() {
    // JWT tokens are stateless, so "invalidation" doesn't actually prevent their use
    UUID userId = UUID.randomUUID();

    // Generate and "invalidate" a token
    String token = authTokenService.generateToken(userId, DEFAULT_PIZZERIA_ID, null).block();
    authTokenService.invalidateToken(token).block();

    // Token is still valid because JWT is stateless
    AuthTokenAuthentication unauthenticated = AuthTokenAuthentication.unauthenticated(token);

    StepVerifier.create(authenticationManager.authenticate(unauthenticated))
        .assertNext(
            auth -> {
              AuthTokenAuthentication authToken = (AuthTokenAuthentication) auth;
              assertThat(authToken.isAuthenticated()).isTrue();
              assertThat(authToken.getUserId()).isEqualTo(userId);
            })
        .verifyComplete();
  }

  @Test
  void authenticateHandlesEmptyToken() {
    AuthTokenAuthentication unauthenticated = AuthTokenAuthentication.unauthenticated("");

    StepVerifier.create(authenticationManager.authenticate(unauthenticated)).verifyComplete();
  }

  @Test
  void tokenFromDifferentPizzeriaCannotAccessOtherPizzeriaData() {
    UUID userId = UUID.randomUUID();

    // Generate token for DEFAULT_PIZZERIA
    String token = authTokenService.generateToken(userId, DEFAULT_PIZZERIA_ID, null).block();

    // Authenticate the token
    AuthTokenAuthentication unauthenticated = AuthTokenAuthentication.unauthenticated(token);

    StepVerifier.create(authenticationManager.authenticate(unauthenticated))
        .assertNext(
            auth -> {
              AuthTokenAuthentication authToken = (AuthTokenAuthentication) auth;
              // Token should be scoped to DEFAULT_PIZZERIA only
              assertThat(authToken.getPizzeriaId()).isEqualTo(DEFAULT_PIZZERIA_ID);
              // Verify it's NOT OTHER_PIZZERIA
              assertThat(authToken.getPizzeriaId()).isNotEqualTo(OTHER_PIZZERIA_ID);
            })
        .verifyComplete();
  }

  @Test
  void authenticatePreservesPizzeriaAdminClaim() {
    UUID userId = UUID.randomUUID();
    String adminPizzeria = "testpizzeria";

    // Generate token with admin claim
    String token =
        authTokenService.generateToken(userId, DEFAULT_PIZZERIA_ID, adminPizzeria).block();

    AuthTokenAuthentication unauthenticated = AuthTokenAuthentication.unauthenticated(token);

    StepVerifier.create(authenticationManager.authenticate(unauthenticated))
        .assertNext(
            auth -> {
              AuthTokenAuthentication authToken = (AuthTokenAuthentication) auth;
              assertThat(authToken.getAuthenticatedUser().pizzeriaAdmin()).isEqualTo(adminPizzeria);
            })
        .verifyComplete();
  }
}
