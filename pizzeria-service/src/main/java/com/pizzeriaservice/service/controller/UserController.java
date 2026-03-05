package com.pizzeriaservice.service.controller;

import com.pizzeriaservice.api.dto.ForgotPasswordRequest;
import com.pizzeriaservice.api.dto.ForgotPasswordResponse;
import com.pizzeriaservice.api.dto.ResetPasswordRequest;
import com.pizzeriaservice.api.dto.UserLoginRequest;
import com.pizzeriaservice.api.dto.UserLoginResponse;
import com.pizzeriaservice.api.dto.UserProfileResponse;
import com.pizzeriaservice.api.dto.UserProfileUpdateRequest;
import com.pizzeriaservice.api.dto.UserRegisterRequest;
import com.pizzeriaservice.api.dto.UserRegisterResponse;
import com.pizzeriaservice.api.dto.UserVerifyEmailRequest;
import com.pizzeriaservice.service.config.CommonApiResponses;
import com.pizzeriaservice.service.service.PizzeriaService;
import com.pizzeriaservice.service.service.UserService;
import com.pizzeriaservice.service.support.UnauthorizedException;
import com.pizzeriaservice.service.support.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "Users", description = "User registration, authentication, and profile management")
@RestController
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final PizzeriaService pizzeriaService;

  // ==================== Public endpoints (pizzeria in URL) ====================

  @Operation(summary = "Register a new user")
  @ApiResponse(responseCode = "201", description = "User registered successfully")
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/api/v1/pizzerias/{pizzeriaCode}/users/register")
  public Mono<UserRegisterResponse> register(
      @Parameter(description = "Pizzeria code identifier") @PathVariable String pizzeriaCode,
      @Valid @RequestBody UserRegisterRequest request) {
    return pizzeriaService
        .resolvePizzeriaId(pizzeriaCode)
        .flatMap(pizzeriaId -> userService.register(request, pizzeriaId));
  }

  @Operation(summary = "Verify user email address")
  @ApiResponse(responseCode = "204", description = "Email verified successfully")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/api/v1/pizzerias/{pizzeriaCode}/users/verify-email")
  public Mono<Void> verifyEmail(
      @Parameter(description = "Pizzeria code identifier") @PathVariable String pizzeriaCode,
      @Valid @RequestBody UserVerifyEmailRequest request) {
    // Verify email doesn't need pizzeriaId validation - token is unique
    return userService.verifyEmail(request);
  }

  @Operation(summary = "Authenticate user and obtain token")
  @ApiResponse(responseCode = "200", description = "Login successful, returns Bearer token")
  @PostMapping("/api/v1/pizzerias/{pizzeriaCode}/users/login")
  public Mono<UserLoginResponse> login(
      @Parameter(description = "Pizzeria code identifier") @PathVariable String pizzeriaCode,
      @Valid @RequestBody UserLoginRequest request) {
    return pizzeriaService
        .resolvePizzeriaId(pizzeriaCode)
        .flatMap(pizzeriaId -> userService.login(request, pizzeriaId));
  }

  @Operation(summary = "Request a password reset token")
  @ApiResponse(responseCode = "200", description = "Password reset token generated")
  @PostMapping("/api/v1/pizzerias/{pizzeriaCode}/users/forgot-password")
  public Mono<ForgotPasswordResponse> forgotPassword(
      @Parameter(description = "Pizzeria code identifier") @PathVariable String pizzeriaCode,
      @Valid @RequestBody ForgotPasswordRequest request) {
    return pizzeriaService
        .resolvePizzeriaId(pizzeriaCode)
        .flatMap(pizzeriaId -> userService.requestPasswordReset(request, pizzeriaId));
  }

  @Operation(summary = "Reset password using a reset token")
  @ApiResponse(responseCode = "204", description = "Password reset successfully")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/api/v1/pizzerias/{pizzeriaCode}/users/reset-password")
  public Mono<Void> resetPassword(
      @Parameter(description = "Pizzeria code identifier") @PathVariable String pizzeriaCode,
      @Valid @RequestBody ResetPasswordRequest request) {
    return userService.resetPassword(request);
  }

  // ==================== Authenticated endpoints (pizzeria from token) ====================

  @Operation(summary = "Invalidate current Bearer token")
  @ApiResponse(responseCode = "204", description = "Logged out successfully")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PostMapping("/api/v1/users/logout")
  public Mono<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      return Mono.error(new UnauthorizedException("Missing authorization"));
    }
    String token =
        authorizationHeader.startsWith("Bearer ")
            ? authorizationHeader.substring(7)
            : authorizationHeader;
    return userService.logout(token);
  }

  @Operation(summary = "Get current user profile")
  @ApiResponse(responseCode = "200", description = "User profile returned")
  @CommonApiResponses
  @GetMapping("/api/v1/users/me")
  public Mono<UserProfileResponse> me(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    return userService.me(user.userId(), user.pizzeriaId());
  }

  @Operation(summary = "Update current user profile")
  @ApiResponse(responseCode = "200", description = "Profile updated successfully")
  @CommonApiResponses
  @PatchMapping("/api/v1/users/me")
  public Mono<UserProfileResponse> updateProfile(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody UserProfileUpdateRequest request) {
    return userService.updateProfile(user.userId(), user.pizzeriaId(), request);
  }

  @Operation(summary = "Delete current user account")
  @ApiResponse(responseCode = "204", description = "Account deleted successfully")
  @CommonApiResponses
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/api/v1/users/me")
  public Mono<Void> deleteMe(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    return userService.deleteUser(user.userId(), user.pizzeriaId());
  }
}
