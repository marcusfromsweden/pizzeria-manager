package com.pizzeriaservice.service.controller;

import com.pizzeriaservice.api.dto.CreateOrderRequest;
import com.pizzeriaservice.api.dto.OrderResponse;
import com.pizzeriaservice.api.dto.OrderSummaryResponse;
import com.pizzeriaservice.service.config.CommonApiResponses;
import com.pizzeriaservice.service.service.OrderService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/v1/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management")
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  @CommonApiResponses
  @Operation(summary = "Create a new order")
  @ApiResponse(responseCode = "201", description = "Order created successfully")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<OrderResponse> createOrder(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody CreateOrderRequest request) {
    return orderService.createOrder(user.userId(), user.pizzeriaId(), request);
  }

  @GetMapping
  @CommonApiResponses
  @Operation(summary = "Get order history")
  @ApiResponse(responseCode = "200", description = "List of past orders returned")
  public Flux<OrderSummaryResponse> getOrderHistory(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    return orderService.getOrderHistory(user.userId(), user.pizzeriaId());
  }

  @GetMapping("/active")
  @CommonApiResponses
  @Operation(summary = "Get active orders")
  @ApiResponse(responseCode = "200", description = "List of active orders returned")
  public Flux<OrderSummaryResponse> getActiveOrders(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    return orderService.getActiveOrders(user.userId(), user.pizzeriaId());
  }

  @GetMapping("/{orderId}")
  @CommonApiResponses
  @Operation(summary = "Get order details")
  @ApiResponse(responseCode = "200", description = "Order details returned")
  public Mono<OrderResponse> getOrder(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Parameter(description = "Order unique identifier") @PathVariable UUID orderId) {
    return orderService.getOrder(orderId, user.userId(), user.pizzeriaId());
  }

  @PostMapping("/{orderId}/cancel")
  @CommonApiResponses
  @Operation(summary = "Cancel an order")
  @ApiResponse(responseCode = "200", description = "Order cancelled successfully")
  public Mono<OrderResponse> cancelOrder(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Parameter(description = "Order unique identifier") @PathVariable UUID orderId) {
    return orderService.cancelOrder(orderId, user.userId(), user.pizzeriaId());
  }
}
