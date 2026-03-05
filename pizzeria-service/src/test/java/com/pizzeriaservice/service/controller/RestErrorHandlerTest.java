package com.pizzeriaservice.service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.service.support.DomainValidationException;
import com.pizzeriaservice.service.support.ErrorCode;
import com.pizzeriaservice.service.support.ResourceNotFoundException;
import com.pizzeriaservice.service.support.UnauthorizedException;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class RestErrorHandlerTest {

  @Mock private MessageSource messageSource;

  private RestErrorHandler handler;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = new RestErrorHandler(messageSource);
  }

  @Test
  void handleDomainValidationReturns422WithDetails() {
    DomainValidationException ex = new DomainValidationException("Email is already registered");
    Locale locale = Locale.ENGLISH;

    when(messageSource.getMessage(
            eq("error.domain.invalid"), any(), eq("Request validation failed"), eq(locale)))
        .thenReturn("Validation failed for the request");

    ProblemDetail detail = handler.handleDomainValidation(ex, locale);

    assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    assertThat(detail.getTitle()).isEqualTo("Validation failed");
    assertThat(detail.getDetail()).isEqualTo("Email is already registered");
    assertThat(detail.getProperties())
        .containsEntry("errorCode", ErrorCode.INVALID_ARGUMENT.name());
    assertThat(detail.getProperties()).containsKey("timestamp");
    assertThat(detail.getProperties())
        .containsEntry("message", "Validation failed for the request");
  }

  @Test
  void handleDomainValidationUsesDefaultMessageWhenKeyNotFound() {
    DomainValidationException ex = new DomainValidationException("Invalid input");
    Locale locale = Locale.ENGLISH;

    when(messageSource.getMessage(
            eq("error.domain.invalid"), any(), eq("Request validation failed"), eq(locale)))
        .thenReturn("Request validation failed");

    ProblemDetail detail = handler.handleDomainValidation(ex, locale);

    assertThat(detail.getProperties()).containsEntry("message", "Request validation failed");
  }

  @Test
  void handleNotFoundReturns404WithDetails() {
    UUID resourceId = UUID.randomUUID();
    ResourceNotFoundException ex = new ResourceNotFoundException("User", resourceId);

    ProblemDetail detail = handler.handleNotFound(ex);

    assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(detail.getTitle()).isEqualTo("Resource not found");
    assertThat(detail.getDetail()).contains("User");
    assertThat(detail.getDetail()).contains(resourceId.toString());
    assertThat(detail.getProperties())
        .containsEntry("errorCode", ErrorCode.RESOURCE_NOT_FOUND.name());
    assertThat(detail.getProperties()).containsKey("timestamp");
  }

  @Test
  void handleNotFoundWithStringIdentifier() {
    ResourceNotFoundException ex = new ResourceNotFoundException("Pizzeria", "marios");

    ProblemDetail detail = handler.handleNotFound(ex);

    assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(detail.getDetail()).contains("Pizzeria");
    assertThat(detail.getDetail()).contains("marios");
  }

  @Test
  void handleUnauthorizedReturns401WithDetails() {
    UnauthorizedException ex = new UnauthorizedException("Invalid credentials");

    ProblemDetail detail = handler.handleUnauthorized(ex);

    assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(detail.getTitle()).isEqualTo("Unauthorized");
    assertThat(detail.getDetail()).isEqualTo("Invalid credentials");
    assertThat(detail.getProperties()).containsEntry("errorCode", ErrorCode.UNAUTHORIZED.name());
    assertThat(detail.getProperties()).containsKey("timestamp");
  }

  @Test
  void handleGenericReturns500WithGenericMessage() {
    Exception ex = new RuntimeException("Something went wrong internally");

    ProblemDetail detail = handler.handleGeneric(ex);

    assertThat(detail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(detail.getTitle()).isEqualTo("Internal error");
    assertThat(detail.getDetail()).isEqualTo("An unexpected error occurred");
    assertThat(detail.getProperties()).containsEntry("errorCode", ErrorCode.INTERNAL_ERROR.name());
    assertThat(detail.getProperties()).containsKey("timestamp");
  }

  @Test
  void handleGenericDoesNotExposeInternalErrorDetails() {
    Exception ex = new RuntimeException("Database connection failed: password=secret123");

    ProblemDetail detail = handler.handleGeneric(ex);

    assertThat(detail.getDetail()).doesNotContain("Database");
    assertThat(detail.getDetail()).doesNotContain("password");
    assertThat(detail.getDetail()).doesNotContain("secret123");
    assertThat(detail.getDetail()).isEqualTo("An unexpected error occurred");
  }
}
