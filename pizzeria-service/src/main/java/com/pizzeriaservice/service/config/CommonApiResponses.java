package com.pizzeriaservice.service.config;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.ProblemDetail;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
  @ApiResponse(
      responseCode = "401",
      description = "Unauthorized – missing or invalid Bearer token",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden – insufficient permissions",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
  @ApiResponse(
      responseCode = "422",
      description = "Unprocessable Entity – validation error",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
  @ApiResponse(
      responseCode = "500",
      description = "Internal Server Error",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
})
public @interface CommonApiResponses {}
