package com.pizzeriaservice.api.dto;

import java.util.UUID;

public record UserRegisterResponse(UUID userId, boolean emailVerified, String verificationToken) {}
