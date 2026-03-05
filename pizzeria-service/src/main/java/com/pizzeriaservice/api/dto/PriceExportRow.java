package com.pizzeriaservice.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PriceExportRow(
    String type, UUID id, String nameKey, BigDecimal priceRegular, BigDecimal priceFamily) {}
