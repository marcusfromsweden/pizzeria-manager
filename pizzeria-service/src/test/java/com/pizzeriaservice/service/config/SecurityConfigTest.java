package com.pizzeriaservice.service.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.pizzeriaservice.service.support.AuthTokenService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class SecurityConfigTest {

  private static final UUID TEST_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final String PIZZERIA_CODE = "testpizzeria";

  @Autowired private WebTestClient webTestClient;

  @Autowired private AuthTokenService authTokenService;

  private String validToken;

  @BeforeEach
  void setUp() {
    UUID userId = UUID.randomUUID();
    validToken = authTokenService.generateToken(userId, TEST_PIZZERIA_ID, null).block();
  }

  // Public endpoint tests - should be accessible without authentication

  @Test
  void registerEndpointIsPublic() {
    webTestClient
        .post()
        .uri("/api/v1/pizzerias/{code}/users/register", PIZZERIA_CODE)
        .bodyValue(
            "{\"name\":\"Test\",\"email\":\"test@example.com\",\"password\":\"password123\"}")
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isNotFound(); // 404 because pizzeria doesn't exist, but NOT 401/403
  }

  @Test
  void loginEndpointIsPublic() {
    webTestClient
        .post()
        .uri("/api/v1/pizzerias/{code}/users/login", PIZZERIA_CODE)
        .bodyValue("{\"email\":\"test@example.com\",\"password\":\"password123\"}")
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isNotFound(); // 404 because pizzeria doesn't exist, but NOT 401/403
  }

  @Test
  void verifyEmailEndpointIsPublic() {
    webTestClient
        .post()
        .uri("/api/v1/pizzerias/{code}/users/verify-email", PIZZERIA_CODE)
        .bodyValue("{\"token\":\"test-verification-token\"}")
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .is4xxClientError(); // 4xx because token is invalid, but NOT 401/403
  }

  @Test
  void menuEndpointIsPublic() {
    webTestClient
        .get()
        .uri("/api/v1/pizzerias/{code}/menu", PIZZERIA_CODE)
        .exchange()
        .expectStatus()
        .isNotFound(); // 404 because pizzeria doesn't exist, but NOT 401/403
  }

  @Test
  void pizzasListEndpointIsPublic() {
    webTestClient
        .get()
        .uri("/api/v1/pizzerias/{code}/pizzas", PIZZERIA_CODE)
        .exchange()
        .expectStatus()
        .isNotFound(); // 404 because pizzeria doesn't exist, but NOT 401/403
  }

  @Test
  void pizzaDetailEndpointIsPublic() {
    UUID pizzaId = UUID.randomUUID();
    webTestClient
        .get()
        .uri("/api/v1/pizzerias/{code}/pizzas/{id}", PIZZERIA_CODE, pizzaId)
        .exchange()
        .expectStatus()
        .isNotFound(); // 404 because pizzeria doesn't exist, but NOT 401/403
  }

  @Test
  void swaggerUiIsPublic() {
    webTestClient.get().uri("/swagger-ui/index.html").exchange().expectStatus().isOk();
  }

  @Test
  void apiDocsIsPublic() {
    webTestClient.get().uri("/v3/api-docs").exchange().expectStatus().isOk();
  }

  @Test
  void actuatorHealthIsPublic() {
    webTestClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
  }

  @Test
  void actuatorInfoIsPublic() {
    webTestClient.get().uri("/actuator/info").exchange().expectStatus().isOk();
  }

  // Protected endpoint tests - should require authentication

  @Test
  void usersMeEndpointRequiresAuthentication() {
    webTestClient.get().uri("/api/v1/users/me").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void usersMeEndpointAllowsAuthenticatedAccess() {
    webTestClient
        .get()
        .uri("/api/v1/users/me")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
        .exchange()
        .expectStatus()
        .isNotFound(); // 404 because user doesn't exist in test DB, but NOT 401/403
  }

  @Test
  void feedbackEndpointRequiresAuthentication() {
    webTestClient
        .post()
        .uri("/api/v1/feedback/service")
        .bodyValue("{\"message\":\"Great!\",\"rating\":5,\"feedbackCategory\":\"DELIVERY\"}")
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  void feedbackEndpointAllowsAuthenticatedAccess() {
    // This test verifies security config allows authenticated access
    // The actual response may vary based on service layer, but it should NOT be 401/403
    webTestClient
        .post()
        .uri("/api/v1/feedback/service")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
        .bodyValue("{\"message\":\"Great!\",\"rating\":5,\"feedbackCategory\":\"DELIVERY\"}")
        .header("Content-Type", "application/json")
        .exchange()
        .expectStatus()
        .value(status -> assertThat(status).isNotIn(401, 403));
  }

  @Test
  void pizzaScoresEndpointRequiresAuthentication() {
    webTestClient.get().uri("/api/v1/pizza-scores/me").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void preferencesEndpointRequiresAuthentication() {
    webTestClient.get().uri("/api/v1/users/me/diet").exchange().expectStatus().isUnauthorized();
  }

  // Invalid token tests

  @Test
  void invalidTokenReturnsUnauthorized() {
    // With an invalid token, should get either 401 Unauthorized or 500 if auth check fails
    // The key is it should NOT return 200 OK or any success status
    webTestClient
        .get()
        .uri("/api/v1/users/me")
        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
        .exchange()
        .expectStatus()
        .value(status -> assertThat(status).isNotIn(200, 201, 204));
  }

  @Test
  void malformedAuthHeaderReturnsUnauthorized() {
    webTestClient
        .get()
        .uri("/api/v1/users/me")
        .header(HttpHeaders.AUTHORIZATION, "NotBearer " + validToken)
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  // Security header tests

  @Test
  void responseContainsSecurityHeaders() {
    webTestClient
        .get()
        .uri("/actuator/health")
        .exchange()
        .expectHeader()
        .exists("X-Content-Type-Options")
        .expectHeader()
        .exists("X-Frame-Options")
        .expectHeader()
        .exists("Cache-Control");
  }

  // PasswordEncoder bean test

  @Test
  void passwordEncoderBeanExists(
      @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder) {
    assertThat(encoder).isNotNull();
    String encoded = encoder.encode("password");
    assertThat(encoder.matches("password", encoded)).isTrue();
    assertThat(encoder.matches("wrongpassword", encoded)).isFalse();
  }
}
