package com.pizzeriaservice.service.controller;

import com.pizzeriaservice.service.support.DomainValidationException;
import com.pizzeriaservice.service.support.ErrorCode;
import com.pizzeriaservice.service.support.ForbiddenException;
import com.pizzeriaservice.service.support.ResourceNotFoundException;
import com.pizzeriaservice.service.support.UnauthorizedException;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class RestErrorHandler {

  private static final Logger log = LoggerFactory.getLogger(RestErrorHandler.class);

  private final MessageSource messageSource;

  @ExceptionHandler(DomainValidationException.class)
  public ProblemDetail handleDomainValidation(DomainValidationException ex, Locale locale) {
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    detail.setTitle("Validation failed");
    detail.setProperty("errorCode", ErrorCode.INVALID_ARGUMENT.name());
    detail.setProperty("timestamp", Instant.now());
    detail.setProperty(
        "message",
        messageSource.getMessage(
            "error.domain.invalid", null, "Request validation failed", locale));
    if (!ex.getViolations().isEmpty()) {
      detail.setProperty("violations", ex.getViolations());
    }
    return detail;
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    detail.setTitle("Resource not found");
    detail.setProperty("errorCode", ErrorCode.RESOURCE_NOT_FOUND.name());
    detail.setProperty("timestamp", Instant.now());
    return detail;
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ProblemDetail handleUnauthorized(UnauthorizedException ex) {
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    detail.setTitle("Unauthorized");
    detail.setProperty("errorCode", ErrorCode.UNAUTHORIZED.name());
    detail.setProperty("timestamp", Instant.now());
    return detail;
  }

  @ExceptionHandler(ForbiddenException.class)
  public ProblemDetail handleForbidden(ForbiddenException ex) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    detail.setTitle("Access denied");
    detail.setProperty("errorCode", ErrorCode.FORBIDDEN.name());
    detail.setProperty("timestamp", Instant.now());
    return detail;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGeneric(Exception ex) {
    log.error("Unhandled exception: {}", ex.getMessage(), ex);
    ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    detail.setTitle("Internal error");
    detail.setDetail("An unexpected error occurred");
    detail.setProperty("errorCode", ErrorCode.INTERNAL_ERROR.name());
    detail.setProperty("timestamp", Instant.now());
    return detail;
  }
}
