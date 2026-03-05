package com.pizzeriaservice.service.support.security;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class AuthTokenAuthentication extends AbstractAuthenticationToken {

  private final AuthenticatedUser authenticatedUser;
  private final String credentials;

  public AuthTokenAuthentication(
      AuthenticatedUser authenticatedUser,
      String token,
      Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.authenticatedUser = authenticatedUser;
    this.credentials = token;
    setAuthenticated(true);
  }

  private AuthTokenAuthentication(String token) {
    super(Collections.emptyList());
    this.authenticatedUser = null;
    this.credentials = token;
    setAuthenticated(false);
  }

  public UUID getUserId() {
    return authenticatedUser != null ? authenticatedUser.userId() : null;
  }

  public UUID getPizzeriaId() {
    return authenticatedUser != null ? authenticatedUser.pizzeriaId() : null;
  }

  public AuthenticatedUser getAuthenticatedUser() {
    return authenticatedUser;
  }

  @Override
  public Object getPrincipal() {
    return authenticatedUser;
  }

  @Override
  public Object getCredentials() {
    return credentials;
  }

  public static AuthTokenAuthentication unauthenticated(String token) {
    return new AuthTokenAuthentication(token);
  }
}
