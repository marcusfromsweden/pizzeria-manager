package com.pizzeriaservice.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PizzaCustomisationResponse(
    UUID id,
    String nameKey,
    BigDecimal priceInSek,
    BigDecimal familySizePriceInSek,
    int sortOrder) {}
