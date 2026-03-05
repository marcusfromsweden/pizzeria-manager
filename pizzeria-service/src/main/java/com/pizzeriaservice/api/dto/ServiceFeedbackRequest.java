package com.pizzeriaservice.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;

public record ServiceFeedbackRequest(
    @NotBlank String message, @Min(1) @Max(5) Integer rating, String category) {
  public Optional<Integer> ratingOptional() {
    return Optional.ofNullable(rating);
  }
}
