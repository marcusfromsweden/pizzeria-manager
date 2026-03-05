package com.pizzeriaservice.service.support;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class DomainValidationException extends RuntimeException {

  private final List<String> violations;

  public DomainValidationException(String message) {
    super(message);
    this.violations = List.of();
  }

  public DomainValidationException(List<String> violations) {
    super("Validation failed: " + String.join("; ", violations));
    this.violations = List.copyOf(violations);
  }

  public List<String> getViolations() {
    return violations;
  }
}
