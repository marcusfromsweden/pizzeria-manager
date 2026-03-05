package com.pizzeriaservice.service.controller;

import com.pizzeriaservice.api.dto.MenuResponse;
import com.pizzeriaservice.service.service.MenuService;
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
@Tag(name = "Menu", description = "Full menu with sections and items")
public class MenuController {

  private final MenuService menuService;
  private final PizzeriaService pizzeriaService;

  @Operation(summary = "Get full menu for pizzeria")
  @ApiResponse(responseCode = "200", description = "Menu returned with all sections and items")
  @GetMapping("/api/v1/pizzerias/{pizzeriaCode}/menu")
  public Mono<MenuResponse> getMenu(
      @Parameter(description = "Pizzeria code identifier") @PathVariable String pizzeriaCode) {
    return pizzeriaService.resolvePizzeriaId(pizzeriaCode).flatMap(menuService::getMenu);
  }
}
