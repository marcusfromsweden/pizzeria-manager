package com.pizzeriaservice.service.service;

import com.pizzeriaservice.api.dto.DietType;
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
import com.pizzeriaservice.service.domain.Diet;
import com.pizzeriaservice.service.domain.UserStatus;
import com.pizzeriaservice.service.domain.model.User;
import com.pizzeriaservice.service.repository.UserRepository;
import com.pizzeriaservice.service.support.AuthTokenService;
import com.pizzeriaservice.service.support.DomainValidationException;
import com.pizzeriaservice.service.support.PasswordResetTokenService;
import com.pizzeriaservice.service.support.ResourceNotFoundException;
import com.pizzeriaservice.service.support.TimeProvider;
import com.pizzeriaservice.service.support.UnauthorizedException;
import com.pizzeriaservice.service.support.UserValidator;
import com.pizzeriaservice.service.support.VerificationTokenService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TimeProvider timeProvider;
  private final VerificationTokenService verificationTokenService;
  private final PasswordResetTokenService passwordResetTokenService;
  private final AuthTokenService authTokenService;
  private final UserValidator userValidator;

  public Mono<UserRegisterResponse> register(UserRegisterRequest request, UUID pizzeriaId) {
    return userRepository
        .findByEmailAndPizzeriaId(request.email(), pizzeriaId)
        .flatMap(
            existing ->
                Mono.<UserRegisterResponse>error(
                    new DomainValidationException("Email is already registered")))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  Instant now = timeProvider.now();
                  UUID userId = UUID.randomUUID();
                  User user =
                      User.builder()
                          .id(userId)
                          .pizzeriaId(pizzeriaId)
                          .name(request.name())
                          .email(request.email())
                          .passwordHash(passwordEncoder.encode(request.password()))
                          .emailVerified(false)
                          .phone(null)
                          .preferredDiet(Diet.NONE)
                          .preferredIngredientIds(new LinkedHashSet<>())
                          .status(UserStatus.ACTIVE)
                          .profilePhotoBase64(null)
                          .createdAt(now)
                          .updatedAt(now)
                          .createdBy(userId)
                          .updatedBy(userId)
                          .isNew(true)
                          .build();
                  return userRepository
                      .save(user)
                      .flatMap(
                          saved ->
                              verificationTokenService
                                  .createToken(saved.id(), saved.pizzeriaId())
                                  .map(
                                      token -> new UserRegisterResponse(saved.id(), false, token)));
                }));
  }

  public Mono<Void> verifyEmail(UserVerifyEmailRequest request) {
    return verificationTokenService
        .consumeToken(request.token())
        .switchIfEmpty(Mono.error(new DomainValidationException("Invalid verification token")))
        .flatMap(
            tokenData ->
                userRepository
                    .findByIdAndPizzeriaId(tokenData.userId(), tokenData.pizzeriaId())
                    .switchIfEmpty(
                        Mono.error(new ResourceNotFoundException("User", tokenData.userId())))
                    .flatMap(
                        user -> {
                          if (user.emailVerified()) {
                            return Mono.empty();
                          }
                          User updated =
                              user.toBuilder()
                                  .emailVerified(true)
                                  .updatedAt(timeProvider.now())
                                  .updatedBy(user.id())
                                  .build();
                          return userRepository.save(updated).then();
                        }));
  }

  public Mono<UserLoginResponse> login(UserLoginRequest request, UUID pizzeriaId) {
    return userRepository
        .findByEmailAndPizzeriaId(request.email(), pizzeriaId)
        .switchIfEmpty(Mono.error(new UnauthorizedException("Invalid credentials")))
        .flatMap(
            user -> {
              if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
                return Mono.error(new UnauthorizedException("Invalid credentials"));
              }
              if (!user.emailVerified()) {
                return Mono.error(new UnauthorizedException("Email verification required"));
              }
              return authTokenService
                  .generateToken(user.id(), user.pizzeriaId(), user.pizzeriaAdmin())
                  .map(UserLoginResponse::new);
            });
  }

  public Mono<Void> logout(String token) {
    return authTokenService.invalidateToken(token);
  }

  public Mono<UserProfileResponse> me(UUID userId, UUID pizzeriaId) {
    return loadUser(userId, pizzeriaId).map(this::toProfileResponse);
  }

  public Mono<UserProfileResponse> updateProfile(
      UUID userId, UUID pizzeriaId, UserProfileUpdateRequest request) {
    return loadUser(userId, pizzeriaId)
        .flatMap(
            existing -> {
              // Validate profile photo if provided
              if (request.profilePhotoBase64() != null) {
                userValidator.validateProfilePhoto(request.profilePhotoBase64());
              }

              // Determine new photo value:
              // - null in request = keep existing
              // - empty string = remove photo
              // - data URL = set new photo
              String newPhoto = existing.profilePhotoBase64();
              if (request.profilePhotoBase64() != null) {
                newPhoto =
                    request.profilePhotoBase64().isEmpty() ? null : request.profilePhotoBase64();
              }

              User updated =
                  existing.toBuilder()
                      .name(Optional.ofNullable(request.name()).orElse(existing.name()))
                      .phone(Optional.ofNullable(request.phone()).orElse(existing.phone()))
                      .profilePhotoBase64(newPhoto)
                      .updatedAt(timeProvider.now())
                      .updatedBy(userId)
                      .build();
              return userRepository.save(updated);
            })
        .map(this::toProfileResponse);
  }

  public Mono<Void> deleteUser(UUID userId, UUID pizzeriaId) {
    return loadUser(userId, pizzeriaId)
        .flatMap(
            user -> {
              if (user.status() == UserStatus.DELETED) {
                return Mono.empty();
              }
              User updated =
                  user.toBuilder()
                      .status(UserStatus.DELETED)
                      .updatedAt(timeProvider.now())
                      .updatedBy(userId)
                      .build();
              return userRepository.save(updated).then();
            });
  }

  public Mono<DietType> getDiet(UUID userId, UUID pizzeriaId) {
    return loadUser(userId, pizzeriaId).map(user -> toDietType(user.preferredDiet()));
  }

  public Mono<DietType> updateDiet(UUID userId, UUID pizzeriaId, DietType dietType) {
    return loadUser(userId, pizzeriaId)
        .flatMap(
            user -> {
              User updated =
                  user.toBuilder()
                      .preferredDiet(fromDietType(dietType))
                      .updatedAt(timeProvider.now())
                      .updatedBy(userId)
                      .build();
              return userRepository.save(updated);
            })
        .map(user -> toDietType(user.preferredDiet()));
  }

  public Mono<Set<UUID>> getPreferredIngredients(UUID userId, UUID pizzeriaId) {
    return loadUser(userId, pizzeriaId).map(User::preferredIngredientIds);
  }

  public Mono<Void> addPreferredIngredient(UUID userId, UUID pizzeriaId, UUID ingredientId) {
    return loadUser(userId, pizzeriaId)
        .flatMap(
            user -> {
              Set<UUID> updatedIds = new LinkedHashSet<>(user.preferredIngredientIds());
              if (!updatedIds.add(ingredientId)) {
                return Mono.empty();
              }
              User updated =
                  user.toBuilder()
                      .preferredIngredientIds(updatedIds)
                      .updatedAt(timeProvider.now())
                      .updatedBy(userId)
                      .build();
              return userRepository.save(updated).then();
            });
  }

  public Mono<Void> removePreferredIngredient(UUID userId, UUID pizzeriaId, UUID ingredientId) {
    return loadUser(userId, pizzeriaId)
        .flatMap(
            user -> {
              Set<UUID> updatedIds = new LinkedHashSet<>(user.preferredIngredientIds());
              if (!updatedIds.remove(ingredientId)) {
                return Mono.empty();
              }
              User updated =
                  user.toBuilder()
                      .preferredIngredientIds(updatedIds)
                      .updatedAt(timeProvider.now())
                      .updatedBy(userId)
                      .build();
              return userRepository.save(updated).then();
            });
  }

  public Mono<ForgotPasswordResponse> requestPasswordReset(
      ForgotPasswordRequest request, UUID pizzeriaId) {
    return userRepository
        .findByEmailAndPizzeriaId(request.email(), pizzeriaId)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException("User with given email not found")))
        .flatMap(
            user ->
                passwordResetTokenService
                    .createToken(user.id(), user.pizzeriaId())
                    .map(ForgotPasswordResponse::new));
  }

  public Mono<Void> resetPassword(ResetPasswordRequest request) {
    return passwordResetTokenService
        .consumeToken(request.token())
        .switchIfEmpty(
            Mono.error(new DomainValidationException("Invalid or expired password reset token")))
        .flatMap(
            tokenData ->
                userRepository
                    .findByIdAndPizzeriaId(tokenData.userId(), tokenData.pizzeriaId())
                    .switchIfEmpty(
                        Mono.error(new ResourceNotFoundException("User", tokenData.userId())))
                    .flatMap(
                        user -> {
                          User updated =
                              user.toBuilder()
                                  .passwordHash(passwordEncoder.encode(request.newPassword()))
                                  .updatedAt(timeProvider.now())
                                  .updatedBy(user.id())
                                  .build();
                          return userRepository.save(updated).then();
                        }));
  }

  private Mono<User> loadUser(UUID userId, UUID pizzeriaId) {
    return userRepository
        .findByIdAndPizzeriaId(userId, pizzeriaId)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", userId)));
  }

  private UserProfileResponse toProfileResponse(User user) {
    return new UserProfileResponse(
        user.id(),
        user.name(),
        user.email(),
        user.emailVerified(),
        toDietType(user.preferredDiet()),
        Set.copyOf(user.preferredIngredientIds()),
        user.pizzeriaAdmin(),
        user.profilePhotoBase64(),
        user.createdAt(),
        user.updatedAt());
  }

  private static Diet fromDietType(DietType dietType) {
    return switch (dietType) {
      case VEGAN -> Diet.VEGAN;
      case VEGETARIAN -> Diet.VEGETARIAN;
      case CARNIVORE -> Diet.CARNIVORE;
      case NONE -> Diet.NONE;
    };
  }

  private static DietType toDietType(Diet diet) {
    return switch (diet) {
      case VEGAN -> DietType.VEGAN;
      case VEGETARIAN -> DietType.VEGETARIAN;
      case CARNIVORE -> DietType.CARNIVORE;
      case NONE -> DietType.NONE;
    };
  }
}
