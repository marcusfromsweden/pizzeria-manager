package com.pizzeriaservice.api.dto;

import java.util.List;

public record PizzaSuitabilityResponse(
    boolean suitable, List<String> violations, List<String> suggestions) {}
