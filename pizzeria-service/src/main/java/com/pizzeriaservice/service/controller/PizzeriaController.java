package com.pizzeriaservice.service.controller;

import com.pizzeriaservice.api.dto.PizzeriaInfoResponse;
import com.pizzeriaservice.service.service.PizzeriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Tag(name = "Pizzerias", description = "Pizzeria information")
public class PizzeriaController {

  private final PizzeriaService pizzeriaService;

  @Operation(summary = "Get pizzeria information")
  @ApiResponse(responseCode = "200", description = "Pizzeria info returned")
  @GetMapping("/api/v1/pizzerias/{pizzeriaCode}")
  public Mono<PizzeriaInfoResponse> getInfo(
      @Parameter(description = "Pizzeria code identifier") @PathVariable String pizzeriaCode) {
    return pizzeriaService.getInfoByCode(pizzeriaCode);
  }
}
