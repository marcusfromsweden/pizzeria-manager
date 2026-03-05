package com.pizzeriaservice.api.dto;

import java.util.List;

public record PriceImportResponse(
    boolean dryRun,
    int totalProcessed,
    int updated,
    int unchanged,
    int errors,
    List<PriceChangeRow> changes) {}
