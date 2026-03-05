package com.pizzeriaservice.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pizzeriaservice.service.support.VerificationTokenService.VerificationTokenData;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class VerificationTokenServiceTest {

  private VerificationTokenService service;

  @BeforeEach
  void setUp() {
    service = new VerificationTokenService();
  }

  @Test
  void createTokenGeneratesBase64UrlEncodedToken() {
    UUID userId = UUID.randomUUID();
    UUID pizzeriaId = UUID.randomUUID();

    StepVerifier.create(service.createToken(userId, pizzeriaId))
        .assertNext(
            token -> {
              assertThat(token).isNotBlank();
              assertThat(token).matches("^[A-Za-z0-9_-]+$");
              assertThat(token.length()).isGreaterThanOrEqualTo(32);
            })
        .verifyComplete();
  }

  @Test
  void createTokenGeneratesUniqueTokens() {
    UUID userId = UUID.randomUUID();
    UUID pizzeriaId = UUID.randomUUID();

    Set<String> tokens = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      String token = service.createToken(userId, pizzeriaId).block();
      assertThat(tokens.add(token)).isTrue();
    }
  }

  @Test
  void consumeTokenReturnsStoredData() {
    UUID userId = UUID.randomUUID();
    UUID pizzeriaId = UUID.randomUUID();

    String token = service.createToken(userId, pizzeriaId).block();

    StepVerifier.create(service.consumeToken(token))
        .assertNext(
            data -> {
              assertThat(data.userId()).isEqualTo(userId);
              assertThat(data.pizzeriaId()).isEqualTo(pizzeriaId);
            })
        .verifyComplete();
  }

  @Test
  void consumeTokenRemovesTokenFromStore() {
    UUID userId = UUID.randomUUID();
    UUID pizzeriaId = UUID.randomUUID();

    String token = service.createToken(userId, pizzeriaId).block();

    service.consumeToken(token).block();

    StepVerifier.create(service.consumeToken(token)).verifyComplete();
  }

  @Test
  void consumeTokenReturnsEmptyForUnknownToken() {
    StepVerifier.create(service.consumeToken("unknown-token")).verifyComplete();
  }

  @Test
  void consumeTokenCannotBeUsedTwice() {
    UUID userId = UUID.randomUUID();
    UUID pizzeriaId = UUID.randomUUID();

    String token = service.createToken(userId, pizzeriaId).block();

    StepVerifier.create(service.consumeToken(token))
        .assertNext(data -> assertThat(data.userId()).isEqualTo(userId))
        .verifyComplete();

    StepVerifier.create(service.consumeToken(token)).verifyComplete();
  }

  @Test
  void peekTokenDataReturnsDataWithoutRemoving() {
    UUID userId = UUID.randomUUID();
    UUID pizzeriaId = UUID.randomUUID();

    String token = service.createToken(userId, pizzeriaId).block();

    StepVerifier.create(service.peekTokenData(token))
        .assertNext(
            data -> {
              assertThat(data.userId()).isEqualTo(userId);
              assertThat(data.pizzeriaId()).isEqualTo(pizzeriaId);
            })
        .verifyComplete();

    StepVerifier.create(service.peekTokenData(token))
        .assertNext(data -> assertThat(data.userId()).isEqualTo(userId))
        .verifyComplete();
  }

  @Test
  void peekTokenDataReturnsEmptyForUnknownToken() {
    StepVerifier.create(service.peekTokenData("unknown-token")).verifyComplete();
  }

  @Test
  void verificationTokenDataRejectsNullUserId() {
    UUID pizzeriaId = UUID.randomUUID();

    assertThatThrownBy(() -> new VerificationTokenData(null, pizzeriaId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("userId cannot be null");
  }

  @Test
  void verificationTokenDataRejectsNullPizzeriaId() {
    UUID userId = UUID.randomUUID();

    assertThatThrownBy(() -> new VerificationTokenData(userId, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("pizzeriaId cannot be null");
  }

  @Test
  void verificationTokenDataStoresBothIds() {
    UUID userId = UUID.randomUUID();
    UUID pizzeriaId = UUID.randomUUID();

    VerificationTokenData data = new VerificationTokenData(userId, pizzeriaId);

    assertThat(data.userId()).isEqualTo(userId);
    assertThat(data.pizzeriaId()).isEqualTo(pizzeriaId);
  }
}
