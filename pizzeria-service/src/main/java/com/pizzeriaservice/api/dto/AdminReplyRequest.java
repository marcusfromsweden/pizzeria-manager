package com.pizzeriaservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminReplyRequest(
    @NotBlank(message = "Reply message is required")
        @Size(max = 2000, message = "Reply must be at most 2000 characters")
        String reply) {}
