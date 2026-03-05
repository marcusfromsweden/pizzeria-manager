package com.pizzeriaservice.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pizzeriaservice.service.support.PasswordResetTokenService.PasswordResetTokenData;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class PasswordResetTokenServiceTest {

  private PasswordResetTokenService service;

  @BeforeEach
  void setUp() {
    service = new PasswordResetTokenService();
  }

  @Test
  void shouldCreateAndConsumeToken() {
    UUID userId = UUID.randomUUID();
    UUID pizzeriaId = UUID.randomUUID();

    String token = service.createToken(userId, pizzeriaId).block();

    assertThat(token).isNotBlank();
    assertThat(token).matches("^[A-Za-z0-9_-]+$");

    StepVerifier.create(service.consumeToken(token))
        .assertNext(
            data -> {
              assertThat(data.userId()).isEqualTo(userId);
              assertThat(data.pizzeriaId()).isEqualTo(pizzeriaId);
            })
        .verifyComplete();
  }

  @Test
  void shouldReturnEmptyForInvalidToken() {
    StepVerifier.create(service.consumeToken("invalid-token")).verifyComplete();
  }

  @Test
  void shouldConsumeTokenOnlyOnce() {
    UUID userId = UUID.randomUUID();
    UUID pizzeriaId = UUID.randomUUID();

    String token = service.createToken(userId, pizzeriaId).block();

    StepVerifier.create(service.consumeToken(token))
        .assertNext(data -> assertThat(data.userId()).isEqualTo(userId))
        .verifyComplete();

    StepVerifier.create(service.consumeToken(token)).verifyComplete();
  }

  @Test
  void passwordResetTokenDataRejectsNullUserId() {
    UUID pizzeriaId = UUID.randomUUID();

    assertThatThrownBy(() -> new PasswordResetTokenData(null, pizzeriaId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("userId cannot be null");
  }

  @Test
  void passwordResetTokenDataRejectsNullPizzeriaId() {
    UUID userId = UUID.randomUUID();

    assertThatThrownBy(() -> new PasswordResetTokenData(userId, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("pizzeriaId cannot be null");
  }
}
