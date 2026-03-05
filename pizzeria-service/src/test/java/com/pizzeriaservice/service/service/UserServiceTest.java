package com.pizzeriaservice.service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pizzeriaservice.api.dto.DietType;
import com.pizzeriaservice.api.dto.ForgotPasswordRequest;
import com.pizzeriaservice.api.dto.ResetPasswordRequest;
import com.pizzeriaservice.api.dto.UserLoginRequest;
import com.pizzeriaservice.api.dto.UserProfileUpdateRequest;
import com.pizzeriaservice.api.dto.UserRegisterRequest;
import com.pizzeriaservice.api.dto.UserVerifyEmailRequest;
import com.pizzeriaservice.service.domain.Diet;
import com.pizzeriaservice.service.domain.model.User;
import com.pizzeriaservice.service.repository.inmemory.InMemoryUserRepository;
import com.pizzeriaservice.service.support.AuthTokenService;
import com.pizzeriaservice.service.support.DomainValidationException;
import com.pizzeriaservice.service.support.PasswordResetTokenService;
import com.pizzeriaservice.service.support.ResourceNotFoundException;
import com.pizzeriaservice.service.support.TimeProvider;
import com.pizzeriaservice.service.support.UnauthorizedException;
import com.pizzeriaservice.service.support.UserValidator;
import com.pizzeriaservice.service.support.VerificationTokenService;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.test.StepVerifier;

class UserServiceTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID OTHER_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final String TEST_JWT_SECRET = "test-secret-key-minimum-32-characters-long";
  private static final long TEST_EXPIRATION_MS = 3600000; // 1 hour

  private InMemoryUserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private TestTimeProvider timeProvider;
  private VerificationTokenService verificationTokenService;
  private PasswordResetTokenService passwordResetTokenService;
  private AuthTokenService authTokenService;
  private UserService userService;

  @BeforeEach
  void setUp() {
    this.userRepository = new InMemoryUserRepository();
    this.passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    this.timeProvider = new TestTimeProvider(Instant.parse("2024-01-01T00:00:00Z"));
    this.verificationTokenService = new VerificationTokenService();
    this.passwordResetTokenService = new PasswordResetTokenService();
    this.authTokenService = new AuthTokenService(TEST_JWT_SECRET, TEST_EXPIRATION_MS);
    this.userService =
        new UserService(
            userRepository,
            passwordEncoder,
            timeProvider,
            verificationTokenService,
            passwordResetTokenService,
            authTokenService,
            new UserValidator());
  }

  @Test
  void registerVerifyAndLoginFlow() {
    UserRegisterRequest request =
        new UserRegisterRequest("Alice", "alice@example.com", "Password123!");

    UserRegisterResponseHolder holder = new UserRegisterResponseHolder();

    StepVerifier.create(userService.register(request, DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              assertThat(response.userId()).isNotNull();
              assertThat(response.emailVerified()).isFalse();
              assertThat(response.verificationToken()).isNotBlank();
              holder.userId = response.userId();
              holder.verificationToken = response.verificationToken();
            })
        .verifyComplete();

    // Verify audit fields are set on registration
    User registered =
        userRepository.findByIdAndPizzeriaId(holder.userId, DEFAULT_PIZZERIA_ID).block();
    assertThat(registered.createdBy()).isEqualTo(holder.userId);
    assertThat(registered.updatedBy()).isEqualTo(holder.userId);

    StepVerifier.create(
            userService.verifyEmail(new UserVerifyEmailRequest(holder.verificationToken)))
        .verifyComplete();

    // Verify updatedBy is set after email verification
    User verified =
        userRepository.findByIdAndPizzeriaId(holder.userId, DEFAULT_PIZZERIA_ID).block();
    assertThat(verified.updatedBy()).isEqualTo(holder.userId);

    StepVerifier.create(
            userService.login(
                new UserLoginRequest(request.email(), request.password()), DEFAULT_PIZZERIA_ID))
        .assertNext(login -> assertThat(login.accessToken()).isNotBlank())
        .verifyComplete();
  }

  @Test
  void registerFailsForDuplicateEmail() {
    UserRegisterRequest request = new UserRegisterRequest("Bob", "bob@example.com", "Password123!");
    StepVerifier.create(userService.register(request, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.userId()).isNotNull())
        .verifyComplete();

    StepVerifier.create(userService.register(request, DEFAULT_PIZZERIA_ID))
        .expectError(DomainValidationException.class)
        .verify();
  }

  @Test
  void loginFailsForUnverifiedEmail() {
    UserRegisterRequest request = new UserRegisterRequest("Eve", "eve@example.com", "Password123!");
    StepVerifier.create(userService.register(request, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.userId()).isNotNull())
        .verifyComplete();

    StepVerifier.create(
            userService.login(
                new UserLoginRequest(request.email(), request.password()), DEFAULT_PIZZERIA_ID))
        .expectError(UnauthorizedException.class)
        .verify();
  }

  @Test
  void managingDietAndPreferredIngredients() {
    UUID ingredientId = UUID.randomUUID();
    UserRegisterRequest request = new UserRegisterRequest("Sam", "sam@example.com", "Password123!");

    UserRegisterResponseHolder holder = new UserRegisterResponseHolder();
    StepVerifier.create(userService.register(request, DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              holder.userId = response.userId();
              holder.verificationToken = response.verificationToken();
            })
        .verifyComplete();

    StepVerifier.create(
            userService.verifyEmail(new UserVerifyEmailRequest(holder.verificationToken)))
        .verifyComplete();

    StepVerifier.create(userService.updateDiet(holder.userId, DEFAULT_PIZZERIA_ID, DietType.VEGAN))
        .assertNext(diet -> assertThat(diet).isEqualTo(DietType.VEGAN))
        .verifyComplete();

    // Verify updatedBy is set after diet update
    User afterDietUpdate =
        userRepository.findByIdAndPizzeriaId(holder.userId, DEFAULT_PIZZERIA_ID).block();
    assertThat(afterDietUpdate.updatedBy()).isEqualTo(holder.userId);

    StepVerifier.create(
            userService.addPreferredIngredient(holder.userId, DEFAULT_PIZZERIA_ID, ingredientId))
        .verifyComplete();

    // Verify updatedBy is set after adding preferred ingredient
    User afterAddIngredient =
        userRepository.findByIdAndPizzeriaId(holder.userId, DEFAULT_PIZZERIA_ID).block();
    assertThat(afterAddIngredient.updatedBy()).isEqualTo(holder.userId);

    StepVerifier.create(userService.getPreferredIngredients(holder.userId, DEFAULT_PIZZERIA_ID))
        .assertNext(set -> assertThat(set).containsExactly(ingredientId))
        .verifyComplete();

    StepVerifier.create(
            userService.removePreferredIngredient(holder.userId, DEFAULT_PIZZERIA_ID, ingredientId))
        .verifyComplete();

    // Verify updatedBy is set after removing preferred ingredient
    User afterRemoveIngredient =
        userRepository.findByIdAndPizzeriaId(holder.userId, DEFAULT_PIZZERIA_ID).block();
    assertThat(afterRemoveIngredient.updatedBy()).isEqualTo(holder.userId);

    StepVerifier.create(userService.getPreferredIngredients(holder.userId, DEFAULT_PIZZERIA_ID))
        .assertNext(set -> assertThat(set).isEmpty())
        .verifyComplete();
  }

  @Test
  void updateProfileUpdatesTimestamps() {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("Initial")
            .email("initial@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .emailVerified(true)
            .preferredDiet(Diet.NONE)
            .preferredIngredientIds(Set.of())
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .build();
    userRepository.save(user).block();

    timeProvider.advance(Duration.ofMinutes(5));
    StepVerifier.create(
            userService.updateProfile(
                user.id(),
                DEFAULT_PIZZERIA_ID,
                new UserProfileUpdateRequest("Updated", "123456789", null)))
        .assertNext(
            profile -> {
              assertThat(profile.name()).isEqualTo("Updated");
              assertThat(profile.updatedAt()).isAfter(profile.createdAt());
            })
        .verifyComplete();

    // Verify updatedBy is set after profile update
    User updated = userRepository.findByIdAndPizzeriaId(user.id(), DEFAULT_PIZZERIA_ID).block();
    assertThat(updated.updatedBy()).isEqualTo(user.id());
  }

  @Test
  void updateProfileWithPhotoSetsPhoto() {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("PhotoUser")
            .email("photouser@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .emailVerified(true)
            .preferredDiet(Diet.NONE)
            .preferredIngredientIds(Set.of())
            .profilePhotoBase64(null)
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .build();
    userRepository.save(user).block();

    timeProvider.advance(Duration.ofMinutes(5));
    String testPhoto = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAA==";

    StepVerifier.create(
            userService.updateProfile(
                user.id(),
                DEFAULT_PIZZERIA_ID,
                new UserProfileUpdateRequest(null, null, testPhoto)))
        .assertNext(
            profile -> {
              assertThat(profile.profilePhotoBase64()).isEqualTo(testPhoto);
              assertThat(profile.updatedAt()).isAfter(profile.createdAt());
            })
        .verifyComplete();
  }

  @Test
  void updateProfileWithEmptyStringRemovesPhoto() {
    String existingPhoto =
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk";
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("RemovePhoto")
            .email("removephoto@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .emailVerified(true)
            .preferredDiet(Diet.NONE)
            .preferredIngredientIds(Set.of())
            .profilePhotoBase64(existingPhoto)
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .build();
    userRepository.save(user).block();

    StepVerifier.create(
            userService.updateProfile(
                user.id(), DEFAULT_PIZZERIA_ID, new UserProfileUpdateRequest(null, null, "")))
        .assertNext(profile -> assertThat(profile.profilePhotoBase64()).isNull())
        .verifyComplete();
  }

  @Test
  void updateProfileRejectsInvalidPhotoFormat() {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("InvalidPhoto")
            .email("invalidphoto@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .emailVerified(true)
            .preferredDiet(Diet.NONE)
            .preferredIngredientIds(Set.of())
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .build();
    userRepository.save(user).block();

    StepVerifier.create(
            userService.updateProfile(
                user.id(),
                DEFAULT_PIZZERIA_ID,
                new UserProfileUpdateRequest(null, null, "not-a-valid-data-url")))
        .expectError(DomainValidationException.class)
        .verify();
  }

  @Test
  void updateProfileRejectsUnsupportedMimeType() {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("BadMime")
            .email("badmime@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .emailVerified(true)
            .preferredDiet(Diet.NONE)
            .preferredIngredientIds(Set.of())
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .build();
    userRepository.save(user).block();

    // SVG is not in the allowed list
    StepVerifier.create(
            userService.updateProfile(
                user.id(),
                DEFAULT_PIZZERIA_ID,
                new UserProfileUpdateRequest(null, null, "data:image/svg+xml;base64,PHN2Zz4=")))
        .expectError(DomainValidationException.class)
        .verify();
  }

  // ==================== Cross-Tenant Isolation Tests ====================

  @Test
  void meFailsWhenAccessingUserFromDifferentPizzeria() {
    // Create user in DEFAULT_PIZZERIA
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("Alice")
            .email("alice@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .emailVerified(true)
            .preferredDiet(Diet.NONE)
            .preferredIngredientIds(Set.of())
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .build();
    userRepository.save(user).block();

    // Attempt to access user from OTHER_PIZZERIA should fail
    StepVerifier.create(userService.me(user.id(), OTHER_PIZZERIA_ID))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  @Test
  void updateProfileFailsWhenAccessingUserFromDifferentPizzeria() {
    // Create user in DEFAULT_PIZZERIA
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("Bob")
            .email("bob@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .emailVerified(true)
            .preferredDiet(Diet.NONE)
            .preferredIngredientIds(Set.of())
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .build();
    userRepository.save(user).block();

    // Attempt to update user from OTHER_PIZZERIA should fail
    StepVerifier.create(
            userService.updateProfile(
                user.id(), OTHER_PIZZERIA_ID, new UserProfileUpdateRequest("Hacked", "999", null)))
        .expectError(ResourceNotFoundException.class)
        .verify();

    // Verify user was not modified
    User unchanged = userRepository.findByIdAndPizzeriaId(user.id(), DEFAULT_PIZZERIA_ID).block();
    assertThat(unchanged.name()).isEqualTo("Bob");
  }

  @Test
  void deleteUserFailsWhenAccessingUserFromDifferentPizzeria() {
    // Create user in DEFAULT_PIZZERIA
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("Charlie")
            .email("charlie@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .emailVerified(true)
            .preferredDiet(Diet.NONE)
            .preferredIngredientIds(Set.of())
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .build();
    userRepository.save(user).block();

    // Attempt to delete user from OTHER_PIZZERIA should fail
    StepVerifier.create(userService.deleteUser(user.id(), OTHER_PIZZERIA_ID))
        .expectError(ResourceNotFoundException.class)
        .verify();

    // Verify user still exists and is not deleted
    User stillExists = userRepository.findByIdAndPizzeriaId(user.id(), DEFAULT_PIZZERIA_ID).block();
    assertThat(stillExists).isNotNull();
  }

  @Test
  void getDietFailsWhenAccessingUserFromDifferentPizzeria() {
    // Create user in DEFAULT_PIZZERIA with VEGAN diet
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("Diana")
            .email("diana@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .emailVerified(true)
            .preferredDiet(Diet.VEGAN)
            .preferredIngredientIds(Set.of())
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .build();
    userRepository.save(user).block();

    // Attempt to get diet from OTHER_PIZZERIA should fail
    StepVerifier.create(userService.getDiet(user.id(), OTHER_PIZZERIA_ID))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  @Test
  void updateDietFailsWhenAccessingUserFromDifferentPizzeria() {
    // Create user in DEFAULT_PIZZERIA
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("Eve")
            .email("eve2@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .emailVerified(true)
            .preferredDiet(Diet.NONE)
            .preferredIngredientIds(Set.of())
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .build();
    userRepository.save(user).block();

    // Attempt to update diet from OTHER_PIZZERIA should fail
    StepVerifier.create(userService.updateDiet(user.id(), OTHER_PIZZERIA_ID, DietType.VEGAN))
        .expectError(ResourceNotFoundException.class)
        .verify();

    // Verify diet was not modified
    User unchanged = userRepository.findByIdAndPizzeriaId(user.id(), DEFAULT_PIZZERIA_ID).block();
    assertThat(unchanged.preferredDiet()).isEqualTo(Diet.NONE);
  }

  @Test
  void getPreferredIngredientsFailsWhenAccessingUserFromDifferentPizzeria() {
    UUID ingredientId = UUID.randomUUID();
    // Create user in DEFAULT_PIZZERIA with preferred ingredient
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("Frank")
            .email("frank@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .emailVerified(true)
            .preferredDiet(Diet.NONE)
            .preferredIngredientIds(Set.of(ingredientId))
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .build();
    userRepository.save(user).block();

    // Attempt to get preferred ingredients from OTHER_PIZZERIA should fail
    StepVerifier.create(userService.getPreferredIngredients(user.id(), OTHER_PIZZERIA_ID))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  @Test
  void addPreferredIngredientFailsWhenAccessingUserFromDifferentPizzeria() {
    // Create user in DEFAULT_PIZZERIA
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("Grace")
            .email("grace@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .emailVerified(true)
            .preferredDiet(Diet.NONE)
            .preferredIngredientIds(Set.of())
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .build();
    userRepository.save(user).block();

    UUID maliciousIngredient = UUID.randomUUID();

    // Attempt to add preferred ingredient from OTHER_PIZZERIA should fail
    StepVerifier.create(
            userService.addPreferredIngredient(user.id(), OTHER_PIZZERIA_ID, maliciousIngredient))
        .expectError(ResourceNotFoundException.class)
        .verify();

    // Verify ingredient was not added
    User unchanged = userRepository.findByIdAndPizzeriaId(user.id(), DEFAULT_PIZZERIA_ID).block();
    assertThat(unchanged.preferredIngredientIds()).isEmpty();
  }

  @Test
  void removePreferredIngredientFailsWhenAccessingUserFromDifferentPizzeria() {
    UUID ingredientId = UUID.randomUUID();
    // Create user in DEFAULT_PIZZERIA with preferred ingredient
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("Henry")
            .email("henry@example.com")
            .passwordHash(passwordEncoder.encode("Password123!"))
            .emailVerified(true)
            .preferredDiet(Diet.NONE)
            .preferredIngredientIds(Set.of(ingredientId))
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .build();
    userRepository.save(user).block();

    // Attempt to remove preferred ingredient from OTHER_PIZZERIA should fail
    StepVerifier.create(
            userService.removePreferredIngredient(user.id(), OTHER_PIZZERIA_ID, ingredientId))
        .expectError(ResourceNotFoundException.class)
        .verify();

    // Verify ingredient was not removed
    User unchanged = userRepository.findByIdAndPizzeriaId(user.id(), DEFAULT_PIZZERIA_ID).block();
    assertThat(unchanged.preferredIngredientIds()).containsExactly(ingredientId);
  }

  @Test
  void loginFailsWhenAccessingUserFromDifferentPizzeria() {
    // Register and verify user in DEFAULT_PIZZERIA
    UserRegisterRequest request =
        new UserRegisterRequest("Ivan", "ivan@example.com", "Password123!");

    UserRegisterResponseHolder holder = new UserRegisterResponseHolder();
    StepVerifier.create(userService.register(request, DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              holder.userId = response.userId();
              holder.verificationToken = response.verificationToken();
            })
        .verifyComplete();

    StepVerifier.create(
            userService.verifyEmail(new UserVerifyEmailRequest(holder.verificationToken)))
        .verifyComplete();

    // Attempt to login from OTHER_PIZZERIA should fail
    StepVerifier.create(
            userService.login(
                new UserLoginRequest(request.email(), request.password()), OTHER_PIZZERIA_ID))
        .expectError(UnauthorizedException.class)
        .verify();

    // Verify login still works from correct pizzeria
    StepVerifier.create(
            userService.login(
                new UserLoginRequest(request.email(), request.password()), DEFAULT_PIZZERIA_ID))
        .assertNext(login -> assertThat(login.accessToken()).isNotBlank())
        .verifyComplete();
  }

  @Test
  void sameEmailCanBeRegisteredInDifferentPizzerias() {
    String sharedEmail = "shared@example.com";
    UserRegisterRequest request = new UserRegisterRequest("User", sharedEmail, "Password123!");

    // Register in DEFAULT_PIZZERIA
    StepVerifier.create(userService.register(request, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.userId()).isNotNull())
        .verifyComplete();

    // Register same email in OTHER_PIZZERIA should succeed
    StepVerifier.create(userService.register(request, OTHER_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.userId()).isNotNull())
        .verifyComplete();

    // Verify both users exist
    assertThat(userRepository.findByEmailAndPizzeriaId(sharedEmail, DEFAULT_PIZZERIA_ID).block())
        .isNotNull();
    assertThat(userRepository.findByEmailAndPizzeriaId(sharedEmail, OTHER_PIZZERIA_ID).block())
        .isNotNull();
  }

  // ==================== Password Reset Tests ====================

  @Test
  void shouldRequestPasswordResetSuccessfully() {
    UserRegisterRequest registerRequest =
        new UserRegisterRequest("ResetUser", "reset@example.com", "Password123!");

    StepVerifier.create(userService.register(registerRequest, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.userId()).isNotNull())
        .verifyComplete();

    StepVerifier.create(
            userService.requestPasswordReset(
                new ForgotPasswordRequest("reset@example.com"), DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.resetToken()).isNotBlank())
        .verifyComplete();
  }

  @Test
  void shouldResetPasswordSuccessfully() {
    UserRegisterRequest registerRequest =
        new UserRegisterRequest("ResetFlow", "resetflow@example.com", "OldPassword123!");

    UserRegisterResponseHolder holder = new UserRegisterResponseHolder();
    StepVerifier.create(userService.register(registerRequest, DEFAULT_PIZZERIA_ID))
        .assertNext(
            response -> {
              holder.userId = response.userId();
              holder.verificationToken = response.verificationToken();
            })
        .verifyComplete();

    StepVerifier.create(
            userService.verifyEmail(new UserVerifyEmailRequest(holder.verificationToken)))
        .verifyComplete();

    // Request password reset
    String[] resetToken = new String[1];
    StepVerifier.create(
            userService.requestPasswordReset(
                new ForgotPasswordRequest("resetflow@example.com"), DEFAULT_PIZZERIA_ID))
        .assertNext(response -> resetToken[0] = response.resetToken())
        .verifyComplete();

    // Reset password
    StepVerifier.create(
            userService.resetPassword(new ResetPasswordRequest(resetToken[0], "NewPassword456!")))
        .verifyComplete();

    // Login with new password should succeed
    StepVerifier.create(
            userService.login(
                new UserLoginRequest("resetflow@example.com", "NewPassword456!"),
                DEFAULT_PIZZERIA_ID))
        .assertNext(login -> assertThat(login.accessToken()).isNotBlank())
        .verifyComplete();

    // Login with old password should fail
    StepVerifier.create(
            userService.login(
                new UserLoginRequest("resetflow@example.com", "OldPassword123!"),
                DEFAULT_PIZZERIA_ID))
        .expectError(UnauthorizedException.class)
        .verify();
  }

  @Test
  void shouldFailResetWithInvalidToken() {
    StepVerifier.create(
            userService.resetPassword(new ResetPasswordRequest("invalid-token", "NewPassword456!")))
        .expectError(DomainValidationException.class)
        .verify();
  }

  @Test
  void shouldFailRequestPasswordResetForUnknownEmail() {
    StepVerifier.create(
            userService.requestPasswordReset(
                new ForgotPasswordRequest("unknown@example.com"), DEFAULT_PIZZERIA_ID))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  @Test
  void shouldFailRequestPasswordResetFromDifferentPizzeria() {
    UserRegisterRequest registerRequest =
        new UserRegisterRequest("CrossTenant", "crosstenant@example.com", "Password123!");

    StepVerifier.create(userService.register(registerRequest, DEFAULT_PIZZERIA_ID))
        .assertNext(response -> assertThat(response.userId()).isNotNull())
        .verifyComplete();

    // Request reset from OTHER_PIZZERIA should fail
    StepVerifier.create(
            userService.requestPasswordReset(
                new ForgotPasswordRequest("crosstenant@example.com"), OTHER_PIZZERIA_ID))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  private static final class UserRegisterResponseHolder {
    UUID userId;
    String verificationToken;
  }

  private static final class TestTimeProvider implements TimeProvider {
    private Instant current;

    private TestTimeProvider(Instant seed) {
      this.current = seed;
    }

    @Override
    public Instant now() {
      return current;
    }

    void advance(Duration duration) {
      current = current.plus(duration);
    }
  }
}
