package com.pizzeriaservice.service.support;

import java.time.Instant;

public interface TimeProvider {
  Instant now();
}
