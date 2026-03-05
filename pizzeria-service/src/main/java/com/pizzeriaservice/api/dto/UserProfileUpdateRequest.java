package com.pizzeriaservice.api.dto;

import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
    @Size(max = 100) String name,
    @Size(max = 30) String phone,
    @Size(max = 700000) String profilePhotoBase64) {}
