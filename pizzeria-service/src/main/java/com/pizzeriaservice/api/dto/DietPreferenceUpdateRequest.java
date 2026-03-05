package com.pizzeriaservice.api.dto;

import jakarta.validation.constraints.NotNull;

public record DietPreferenceUpdateRequest(@NotNull DietType diet) {}
