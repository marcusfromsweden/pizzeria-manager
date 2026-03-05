package com.pizzeriaservice.service.support.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthTokenServerAuthenticationConverter implements ServerAuthenticationConverter {

  private static final Logger log =
      LoggerFactory.getLogger(AuthTokenServerAuthenticationConverter.class);

  @Override
  public Mono<Authentication> convert(ServerWebExchange exchange) {
    String path = exchange.getRequest().getPath().value();
    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    log.info(
        "Converting auth for path: {}, Authorization header present: {}", path, authHeader != null);
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      log.info("No valid Bearer token found for path: {}", path);
      return Mono.empty();
    }
    String token = authHeader.substring(7);
    log.info("Token extracted for path: {}, token length: {}", path, token.length());
    return Mono.just(AuthTokenAuthentication.unauthenticated(token));
  }
}
