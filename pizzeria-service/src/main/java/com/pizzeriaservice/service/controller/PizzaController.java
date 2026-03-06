package com.pizzeriaservice.service.controller;

import com.pizzeriaservice.api.dto.PizzaDetailResponse;
import com.pizzeriaservice.api.dto.PizzaSuitabilityRequest;
import com.pizzeriaservice.api.dto.PizzaSuitabilityResponse;
import com.pizzeriaservice.api.dto.PizzaSummaryResponse;
import com.pizzeriaservice.service.config.CommonApiResponses;
import com.pizzeriaservice.service.service.MenuService;
import com.pizzeriaservice.service.service.PizzaService;
import com.pizzeriaservice.service.service.PizzeriaService;
import com.pizzeriaservice.service.support.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Tag(name = "Pizzas", description = "Pizza catalog and dietary suitability")
public class PizzaController {

  private final PizzaService pizzaService;
  private final MenuService menuService;
  private final PizzeriaService pizzeriaService;

  // ==================== Public endpoints (pizzeria in URL) ====================

  @Operation(summary = "List all pizzas")
  @ApiResponse(responseCode = "200", description = "List of pizzas returned")
  @GetMapping("/api/v1/pizzerias/{pizzeriaCode}/pizzas")
  public Flux<PizzaSummaryResponse> list(
      @Parameter(description = "Pizzeria code identifier") @PathVariable String pizzeriaCode) {
    return pizzeriaService
        .resolvePizzeriaId(pizzeriaCode)
        .flatMap(menuService::getMenu)
        .flatMapMany(
            menu ->
                Flux.fromIterable(
                    menu.sections().stream()
                        .filter(section -> section.code().contains("pizza"))
                        .flatMap(section -> section.items().stream())
                        .map(
                            item ->
                                new PizzaSummaryResponse(
                                    item.id(),
                                    item.dishNumber(),
                                    item.nameKey(),
                                    item.priceInSek(),
                                    item.familySizePriceInSek(),
                                    item.overallDietaryType(),
                                    item.sortOrder(),
                                    item.totalCalories()))
                        .toList()));
  }

  @Operation(summary = "Get pizza details by ID")
  @ApiResponse(responseCode = "200", description = "Pizza details returned")
  @GetMapping("/api/v1/pizzerias/{pizzeriaCode}/pizzas/{pizzaId}")
  public Mono<PizzaDetailResponse> get(
      @Parameter(description = "Pizzeria code identifier") @PathVariable String pizzeriaCode,
      @Parameter(description = "Pizza unique identifier") @PathVariable UUID pizzaId) {
    return pizzeriaService
        .resolvePizzeriaId(pizzeriaCode)
        .flatMap(pizzeriaId -> pizzaService.get(pizzaId, pizzeriaId));
  }

  // ==================== Authenticated endpoints (pizzeria from token) ====================

  @Operation(summary = "Check pizza suitability for user diet")
  @ApiResponse(responseCode = "200", description = "Suitability check result returned")
  @CommonApiResponses
  @PostMapping("/api/v1/pizzas/suitability")
  public Mono<PizzaSuitabilityResponse> suitability(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody PizzaSuitabilityRequest request) {
    return pizzaService.suitability(request, user.userId(), user.pizzeriaId());
  }
}
