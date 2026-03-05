package com.pizzeriaservice.service.support;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(String resource, UUID id) {
    super(resource + " with id '%s' not found".formatted(id));
  }

  public ResourceNotFoundException(String resource, String code) {
    super(resource + " with code '%s' not found".formatted(code));
  }

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
