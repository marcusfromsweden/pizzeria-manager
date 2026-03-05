package com.pizzeriaservice.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record OrderItemRequest(
    @NotNull UUID menuItemId,
    @NotNull String size,
    @Min(1) int quantity,
    List<UUID> customisationIds,
    String specialInstructions) {}
