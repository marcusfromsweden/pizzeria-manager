package com.pizzeriaservice.service.controller;

import com.pizzeriaservice.api.dto.DeliveryAddressResponse;
import com.pizzeriaservice.api.dto.SaveDeliveryAddressRequest;
import com.pizzeriaservice.service.config.CommonApiResponses;
import com.pizzeriaservice.service.service.DeliveryAddressService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/v1/users/me/addresses", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Delivery Addresses", description = "User delivery address management")
public class DeliveryAddressController {

  private final DeliveryAddressService addressService;

  @GetMapping
  @CommonApiResponses
  @Operation(summary = "Get all delivery addresses")
  @ApiResponse(responseCode = "200", description = "List of delivery addresses returned")
  public Flux<DeliveryAddressResponse> getAddresses(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    return addressService.getAddresses(user.userId(), user.pizzeriaId());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @CommonApiResponses
  @Operation(summary = "Save a delivery address")
  @ApiResponse(responseCode = "201", description = "Address saved successfully")
  public Mono<DeliveryAddressResponse> saveAddress(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody SaveDeliveryAddressRequest request) {
    return addressService.saveAddress(user.userId(), user.pizzeriaId(), request);
  }

  @DeleteMapping("/{addressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @CommonApiResponses
  @Operation(summary = "Delete a delivery address")
  @ApiResponse(responseCode = "204", description = "Address deleted successfully")
  public Mono<Void> deleteAddress(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Parameter(description = "Address unique identifier") @PathVariable UUID addressId) {
    return addressService.deleteAddress(addressId, user.userId(), user.pizzeriaId());
  }

  @PostMapping("/{addressId}/default")
  @CommonApiResponses
  @Operation(summary = "Set address as default")
  @ApiResponse(responseCode = "200", description = "Address set as default")
  public Mono<DeliveryAddressResponse> setDefaultAddress(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Parameter(description = "Address unique identifier") @PathVariable UUID addressId) {
    return addressService.setDefaultAddress(addressId, user.userId(), user.pizzeriaId());
  }
}
