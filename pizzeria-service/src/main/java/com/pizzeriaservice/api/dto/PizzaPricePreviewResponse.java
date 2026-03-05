package com.pizzeriaservice.api.dto;

import java.math.BigDecimal;

public record PizzaPricePreviewResponse(
    BigDecimal originalPrice, BigDecimal newPrice, BigDecimal delta) {}
