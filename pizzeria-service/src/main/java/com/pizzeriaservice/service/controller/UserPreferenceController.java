package com.pizzeriaservice.service.controller;

import com.pizzeriaservice.api.dto.DietPreferenceResponse;
import com.pizzeriaservice.api.dto.DietPreferenceUpdateRequest;
import com.pizzeriaservice.api.dto.IngredientIdResponse;
import com.pizzeriaservice.api.dto.PreferredIngredientRequest;
import com.pizzeriaservice.service.config.CommonApiResponses;
import com.pizzeriaservice.service.service.UserService;
import com.pizzeriaservice.service.support.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/v1/users/me", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "User Preferences", description = "Dietary preferences and preferred ingredients")
public class UserPreferenceController {

  private final UserService userService;

  @GetMapping("/diet")
  @Operation(summary = "Get dietary preference")
  @ApiResponse(responseCode = "200", description = "Diet preference returned")
  @CommonApiResponses
  public Mono<DietPreferenceResponse> getDiet(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    return userService.getDiet(user.userId(), user.pizzeriaId()).map(DietPreferenceResponse::new);
  }

  @PutMapping("/diet")
  @Operation(summary = "Update dietary preference")
  @ApiResponse(responseCode = "200", description = "Diet preference updated")
  @CommonApiResponses
  public Mono<DietPreferenceResponse> updateDiet(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody DietPreferenceUpdateRequest request) {
    return userService
        .updateDiet(user.userId(), user.pizzeriaId(), request.diet())
        .map(DietPreferenceResponse::new);
  }

  @GetMapping("/preferences/ingredients/preferred")
  @Operation(summary = "Get preferred ingredients")
  @ApiResponse(responseCode = "200", description = "List of preferred ingredient IDs returned")
  @CommonApiResponses
  public Flux<IngredientIdResponse> preferredIngredients(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    return userService
        .getPreferredIngredients(user.userId(), user.pizzeriaId())
        .flatMapMany(set -> Flux.fromIterable(set).map(IngredientIdResponse::new));
  }

  @PostMapping("/preferences/ingredients/preferred")
  @Operation(summary = "Add a preferred ingredient")
  @ApiResponse(responseCode = "201", description = "Ingredient added to preferences")
  @CommonApiResponses
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<Void> addPreferredIngredient(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody PreferredIngredientRequest request) {
    return userService.addPreferredIngredient(
        user.userId(), user.pizzeriaId(), request.ingredientId());
  }

  @DeleteMapping("/preferences/ingredients/preferred/{ingredientId}")
  @Operation(summary = "Remove a preferred ingredient")
  @ApiResponse(responseCode = "204", description = "Ingredient removed from preferences")
  @CommonApiResponses
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> removePreferredIngredient(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Parameter(description = "Ingredient unique identifier") @PathVariable UUID ingredientId) {
    return userService.removePreferredIngredient(user.userId(), user.pizzeriaId(), ingredientId);
  }
}
