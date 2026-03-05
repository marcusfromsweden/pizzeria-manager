package com.pizzeriaservice.service.config;

import com.pizzeriaservice.service.support.security.AuthTokenAuthenticationManager;
import com.pizzeriaservice.service.support.security.AuthTokenServerAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity(useAuthorizationManager = true)
public class SecurityConfig {

  @Bean
  SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http,
      AuthTokenAuthenticationManager authenticationManager,
      AuthTokenServerAuthenticationConverter converter) {

    AuthenticationWebFilter authenticationWebFilter =
        new AuthenticationWebFilter(authenticationManager);
    authenticationWebFilter.setServerAuthenticationConverter(converter);
    authenticationWebFilter.setSecurityContextRepository(
        NoOpServerSecurityContextRepository.getInstance());

    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .logout(ServerHttpSecurity.LogoutSpec::disable)
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                    .hsts(hsts -> hsts.includeSubdomains(true)))
        .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        .authorizeExchange(
            exchanges ->
                exchanges
                    // Public pizzeria-scoped endpoints (pizzeria code in URL)
                    .pathMatchers(
                        HttpMethod.POST,
                        "/api/v1/pizzerias/*/users/register",
                        "/api/v1/pizzerias/*/users/login",
                        "/api/v1/pizzerias/*/users/verify-email",
                        "/api/v1/pizzerias/*/users/forgot-password",
                        "/api/v1/pizzerias/*/users/reset-password")
                    .permitAll()
                    .pathMatchers(
                        HttpMethod.GET,
                        "/api/v1/pizzerias/*",
                        "/api/v1/pizzerias/*/menu",
                        "/api/v1/pizzerias/*/pizzas",
                        "/api/v1/pizzerias/*/pizzas/**",
                        "/api/v1/pizzerias/*/ingredients/**")
                    .permitAll()
                    // Swagger and actuator
                    .pathMatchers(
                        HttpMethod.GET,
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**")
                    .permitAll()
                    .pathMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .anyExchange()
                    .authenticated())
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}
