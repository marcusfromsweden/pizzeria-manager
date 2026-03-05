package com.pizzeriaservice.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UserVerifyEmailRequest(@NotBlank String token) {}
