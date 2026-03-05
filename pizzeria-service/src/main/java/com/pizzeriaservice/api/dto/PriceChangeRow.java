package com.pizzeriaservice.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PriceChangeRow(
    String type,
    UUID id,
    String nameKey,
    BigDecimal oldPriceRegular,
    BigDecimal newPriceRegular,
    BigDecimal oldPriceFamily,
    BigDecimal newPriceFamily,
    String status) {}
