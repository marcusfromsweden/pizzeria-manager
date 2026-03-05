package com.pizzeriaservice.service.support.security;

import com.pizzeriaservice.service.support.AuthTokenService;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthTokenAuthenticationManager implements ReactiveAuthenticationManager {

  private static final Logger log = LoggerFactory.getLogger(AuthTokenAuthenticationManager.class);

  private final AuthTokenService authTokenService;

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    if (!(authentication instanceof AuthTokenAuthentication authToken)) {
      log.info(
          "Authentication is not AuthTokenAuthentication: {}", authentication.getClass().getName());
      return Mono.empty();
    }
    String token = (String) authToken.getCredentials();
    log.info("Authenticating token, length: {}", token.length());
    return authTokenService
        .resolveUser(token)
        .doOnNext(user -> log.info("Token resolved to user: {}", user.userId()))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info("Token not found in active tokens");
                  return Mono.empty();
                }))
        .map(
            authenticatedUser ->
                new AuthTokenAuthentication(authenticatedUser, token, Collections.emptyList()));
  }
}
