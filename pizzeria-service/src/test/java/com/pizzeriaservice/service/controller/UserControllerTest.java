package com.pizzeriaservice.service.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.api.dto.DietType;
import com.pizzeriaservice.api.dto.UserLoginRequest;
import com.pizzeriaservice.api.dto.UserLoginResponse;
import com.pizzeriaservice.api.dto.UserProfileResponse;
import com.pizzeriaservice.api.dto.UserProfileUpdateRequest;
import com.pizzeriaservice.api.dto.UserRegisterRequest;
import com.pizzeriaservice.api.dto.UserRegisterResponse;
import com.pizzeriaservice.api.dto.UserVerifyEmailRequest;
import com.pizzeriaservice.service.service.PizzeriaService;
import com.pizzeriaservice.service.service.UserService;
import com.pizzeriaservice.service.support.UnauthorizedException;
import com.pizzeriaservice.service.support.security.AuthenticatedUser;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class UserControllerTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final String PIZZERIA_CODE = "testpizzeria";

  @Mock private UserService userService;
  @Mock private PizzeriaService pizzeriaService;

  private UserController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new UserController(userService, pizzeriaService);
  }

  @Test
  void registerDelegatesToServiceAndReturns201() {
    UserRegisterRequest request =
        new UserRegisterRequest("Alice", "alice@example.com", "Password123!");
    UUID userId = UUID.randomUUID();
    UserRegisterResponse response = new UserRegisterResponse(userId, false, "verification-token");

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(userService.register(request, DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(response));

    StepVerifier.create(controller.register(PIZZERIA_CODE, request))
        .expectNext(response)
        .verifyComplete();

    verify(pizzeriaService).resolvePizzeriaId(PIZZERIA_CODE);
    verify(userService).register(request, DEFAULT_PIZZERIA_ID);
  }

  @Test
  void verifyEmailDelegatesToServiceAndReturnsNoContent() {
    UserVerifyEmailRequest request = new UserVerifyEmailRequest("verification-token");

    when(userService.verifyEmail(request)).thenReturn(Mono.empty());

    StepVerifier.create(controller.verifyEmail(PIZZERIA_CODE, request)).verifyComplete();

    verify(userService).verifyEmail(request);
  }

  @Test
  void loginDelegatesToService() {
    UserLoginRequest request = new UserLoginRequest("alice@example.com", "Password123!");
    UserLoginResponse response = new UserLoginResponse("access-token");

    when(pizzeriaService.resolvePizzeriaId(PIZZERIA_CODE))
        .thenReturn(Mono.just(DEFAULT_PIZZERIA_ID));
    when(userService.login(request, DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(response));

    StepVerifier.create(controller.login(PIZZERIA_CODE, request))
        .expectNext(response)
        .verifyComplete();

    verify(pizzeriaService).resolvePizzeriaId(PIZZERIA_CODE);
    verify(userService).login(request, DEFAULT_PIZZERIA_ID);
  }

  @Test
  void logoutDelegatesToServiceAndReturnsNoContent() {
    String token = "access-token";
    String authHeader = "Bearer " + token;

    when(userService.logout(token)).thenReturn(Mono.empty());

    StepVerifier.create(controller.logout(authHeader)).verifyComplete();

    verify(userService).logout(token);
  }

  @Test
  void logoutExtractsTokenWithoutBearerPrefix() {
    String token = "access-token";

    when(userService.logout(token)).thenReturn(Mono.empty());

    StepVerifier.create(controller.logout(token)).verifyComplete();

    verify(userService).logout(token);
  }

  @Test
  void logoutReturnsUnauthorizedForNullHeader() {
    StepVerifier.create(controller.logout(null)).expectError(UnauthorizedException.class).verify();
  }

  @Test
  void logoutReturnsUnauthorizedForBlankHeader() {
    StepVerifier.create(controller.logout("   ")).expectError(UnauthorizedException.class).verify();
  }

  @Test
  void meDelegatesToService() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);
    Instant now = Instant.now();
    UserProfileResponse response =
        new UserProfileResponse(
            userId,
            "Alice",
            "alice@example.com",
            true,
            DietType.NONE,
            Set.of(),
            null,
            null,
            now,
            now);

    when(userService.me(userId, DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(response));

    StepVerifier.create(controller.me(user)).expectNext(response).verifyComplete();

    verify(userService).me(userId, DEFAULT_PIZZERIA_ID);
  }

  @Test
  void updateProfileDelegatesToService() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);
    UserProfileUpdateRequest request = new UserProfileUpdateRequest("Updated", "123456789", null);
    Instant now = Instant.now();
    UserProfileResponse response =
        new UserProfileResponse(
            userId,
            "Updated",
            "alice@example.com",
            true,
            DietType.NONE,
            Set.of(),
            null,
            null,
            now,
            now);

    when(userService.updateProfile(userId, DEFAULT_PIZZERIA_ID, request))
        .thenReturn(Mono.just(response));

    StepVerifier.create(controller.updateProfile(user, request))
        .expectNext(response)
        .verifyComplete();

    verify(userService).updateProfile(userId, DEFAULT_PIZZERIA_ID, request);
  }

  @Test
  void deleteMeDelegatesToServiceAndReturnsNoContent() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);

    when(userService.deleteUser(userId, DEFAULT_PIZZERIA_ID)).thenReturn(Mono.empty());

    StepVerifier.create(controller.deleteMe(user)).verifyComplete();

    verify(userService).deleteUser(userId, DEFAULT_PIZZERIA_ID);
  }
}
