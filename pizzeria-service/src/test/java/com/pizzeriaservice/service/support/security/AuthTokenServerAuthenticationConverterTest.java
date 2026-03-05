package com.pizzeriaservice.service.support.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

class AuthTokenServerAuthenticationConverterTest {

  private AuthTokenServerAuthenticationConverter converter;
  private ServerWebExchange exchange;
  private ServerHttpRequest request;
  private HttpHeaders headers;

  @BeforeEach
  void setUp() {
    converter = new AuthTokenServerAuthenticationConverter();
    exchange = mock(ServerWebExchange.class);
    request = mock(ServerHttpRequest.class);
    headers = new HttpHeaders();
    RequestPath path = mock(RequestPath.class);

    when(exchange.getRequest()).thenReturn(request);
    when(request.getHeaders()).thenReturn(headers);
    when(request.getPath()).thenReturn(path);
    when(path.value()).thenReturn("/test/path");
  }

  @Test
  void convertReturnsUnauthenticatedTokenForValidBearerHeader() {
    String token = "valid-token-string";
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);

    StepVerifier.create(converter.convert(exchange))
        .assertNext(
            auth -> {
              assertThat(auth).isInstanceOf(AuthTokenAuthentication.class);
              AuthTokenAuthentication authToken = (AuthTokenAuthentication) auth;
              assertThat(authToken.getCredentials()).isEqualTo(token);
              assertThat(authToken.isAuthenticated()).isFalse();
            })
        .verifyComplete();
  }

  @Test
  void convertReturnsEmptyForMissingAuthHeader() {
    // No Authorization header set

    StepVerifier.create(converter.convert(exchange)).verifyComplete();
  }

  @Test
  void convertReturnsEmptyForNonBearerAuthHeader() {
    headers.set(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNzd29yZA==");

    StepVerifier.create(converter.convert(exchange)).verifyComplete();
  }

  @Test
  void convertReturnsEmptyForMalformedBearerHeader() {
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer"); // No space and token

    StepVerifier.create(converter.convert(exchange)).verifyComplete();
  }

  @Test
  void convertHandlesBearerWithExtraSpaces() {
    // "Bearer  token" with extra space - substring(7) will include leading space
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer  token-with-leading-space");

    StepVerifier.create(converter.convert(exchange))
        .assertNext(
            auth -> {
              AuthTokenAuthentication authToken = (AuthTokenAuthentication) auth;
              // Token includes the extra space
              assertThat(authToken.getCredentials()).isEqualTo(" token-with-leading-space");
            })
        .verifyComplete();
  }

  @Test
  void convertHandlesEmptyTokenAfterBearer() {
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer ");

    StepVerifier.create(converter.convert(exchange))
        .assertNext(
            auth -> {
              AuthTokenAuthentication authToken = (AuthTokenAuthentication) auth;
              assertThat((String) authToken.getCredentials()).isEmpty();
            })
        .verifyComplete();
  }

  @Test
  void convertIsCaseSensitiveForBearer() {
    headers.set(HttpHeaders.AUTHORIZATION, "bearer token");

    // "bearer" lowercase should not match
    StepVerifier.create(converter.convert(exchange)).verifyComplete();
  }

  @Test
  void convertHandlesLongToken() {
    String longToken = "a".repeat(1000);
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + longToken);

    StepVerifier.create(converter.convert(exchange))
        .assertNext(
            auth -> {
              AuthTokenAuthentication authToken = (AuthTokenAuthentication) auth;
              assertThat(authToken.getCredentials()).isEqualTo(longToken);
            })
        .verifyComplete();
  }

  @Test
  void convertHandlesTokenWithSpecialCharacters() {
    String specialToken = "abc123_-./+=";
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + specialToken);

    StepVerifier.create(converter.convert(exchange))
        .assertNext(
            auth -> {
              AuthTokenAuthentication authToken = (AuthTokenAuthentication) auth;
              assertThat(authToken.getCredentials()).isEqualTo(specialToken);
            })
        .verifyComplete();
  }

  @Test
  void convertReturnsEmptyForOtherAuthSchemes() {
    // Test various non-Bearer schemes
    String[] schemes = {"Digest", "NTLM", "Negotiate", "OAuth", "JWT"};

    for (String scheme : schemes) {
      headers.set(HttpHeaders.AUTHORIZATION, scheme + " some-token");

      StepVerifier.create(converter.convert(exchange)).verifyComplete();
    }
  }
}
