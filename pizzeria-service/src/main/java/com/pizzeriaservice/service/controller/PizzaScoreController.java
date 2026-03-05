package com.pizzeriaservice.service.controller;

import com.pizzeriaservice.api.dto.PizzaScoreCreateRequest;
import com.pizzeriaservice.api.dto.PizzaScoreResponse;
import com.pizzeriaservice.service.config.CommonApiResponses;
import com.pizzeriaservice.service.service.PizzaScoreService;
import com.pizzeriaservice.service.support.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/v1/pizza-scores", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Pizza Scores", description = "Pizza ratings and scores")
public class PizzaScoreController {

  private final PizzaScoreService pizzaScoreService;

  @PostMapping
  @Operation(summary = "Rate a pizza")
  @ApiResponse(responseCode = "201", description = "Pizza score created")
  @CommonApiResponses
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<PizzaScoreResponse> createScore(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody PizzaScoreCreateRequest request) {
    return pizzaScoreService.create(user.userId(), user.pizzeriaId(), request);
  }

  @GetMapping("/me")
  @Operation(summary = "Get current user's pizza scores")
  @ApiResponse(responseCode = "200", description = "List of user's pizza scores returned")
  @CommonApiResponses
  public Flux<PizzaScoreResponse> myScores(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    return pizzaScoreService.getForUser(user.userId(), user.pizzeriaId());
  }
}
