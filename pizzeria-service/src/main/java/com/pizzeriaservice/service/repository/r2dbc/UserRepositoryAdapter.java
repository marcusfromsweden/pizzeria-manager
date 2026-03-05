package com.pizzeriaservice.service.repository.r2dbc;

import com.pizzeriaservice.service.domain.Diet;
import com.pizzeriaservice.service.domain.UserStatus;
import com.pizzeriaservice.service.domain.model.User;
import com.pizzeriaservice.service.repository.UserRepository;
import com.pizzeriaservice.service.user.UserEntity;
import com.pizzeriaservice.service.user.UserPreferredIngredientEntity;
import com.pizzeriaservice.service.user.UserPreferredIngredientRepository;
import com.pizzeriaservice.service.user.UserRepositoryR2dbc;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

  private final UserRepositoryR2dbc userRepository;
  private final UserPreferredIngredientRepository preferredRepository;

  @Override
  public Mono<User> save(User user) {
    UserEntity entity = toEntity(user);
    return userRepository
        .save(entity)
        .flatMap(
            saved ->
                syncPreferredIngredients(
                        saved.getPizzeriaId(),
                        saved.getId(),
                        user.preferredIngredientIds(),
                        user.updatedAt())
                    .thenReturn(saved.getId()))
        .flatMap(this::fetchUser);
  }

  @Override
  public Mono<User> findByIdAndPizzeriaId(UUID id, UUID pizzeriaId) {
    return userRepository
        .findByIdAndPizzeriaId(id, pizzeriaId)
        .flatMap(entity -> fetchUser(entity.getId()));
  }

  @Override
  public Mono<User> findByEmailAndPizzeriaId(String email, UUID pizzeriaId) {
    return userRepository
        .findByEmailAndPizzeriaId(email, pizzeriaId)
        .flatMap(entity -> fetchUser(entity.getId()));
  }

  @Override
  public Flux<User> findAllByPizzeriaId(UUID pizzeriaId) {
    return userRepository
        .findAllByPizzeriaId(pizzeriaId)
        .flatMap(
            entity ->
                preferredRepository
                    .findAllByUserId(entity.getId())
                    .map(UserPreferredIngredientEntity::getIngredientId)
                    .collectList()
                    .map(preferred -> toDomain(entity, preferred)));
  }

  @Override
  public Mono<Void> deleteById(UUID id) {
    return preferredRepository.deleteByUserId(id).then().then(userRepository.deleteById(id));
  }

  private Mono<User> fetchUser(UUID userId) {
    return userRepository
        .findById(userId)
        .flatMap(
            entity ->
                preferredRepository
                    .findAllByUserId(userId)
                    .map(UserPreferredIngredientEntity::getIngredientId)
                    .collectList()
                    .map(preferred -> toDomain(entity, preferred)));
  }

  private Mono<Void> syncPreferredIngredients(
      UUID pizzeriaId, UUID userId, Set<UUID> preferredIds, Instant timestamp) {
    Set<UUID> desired = preferredIds != null ? new LinkedHashSet<>(preferredIds) : Set.of();
    return preferredRepository
        .findAllByUserId(userId)
        .collectList()
        .flatMap(
            existing -> {
              Set<UUID> current =
                  existing.stream()
                      .map(UserPreferredIngredientEntity::getIngredientId)
                      .collect(Collectors.toSet());

              List<UUID> toRemove = current.stream().filter(id -> !desired.contains(id)).toList();

              List<UUID> toAdd = desired.stream().filter(id -> !current.contains(id)).toList();

              Mono<Void> removeMono =
                  Flux.fromIterable(toRemove)
                      .flatMap(id -> preferredRepository.deleteByUserIdAndIngredientId(userId, id))
                      .then();

              Mono<Void> addMono =
                  Flux.fromIterable(toAdd)
                      .flatMap(
                          id ->
                              preferredRepository.save(
                                  UserPreferredIngredientEntity.builder()
                                      .id(UUID.randomUUID())
                                      .pizzeriaId(pizzeriaId)
                                      .userId(userId)
                                      .ingredientId(id)
                                      .createdAt(timestamp != null ? timestamp : Instant.now())
                                      .isNew(true)
                                      .build()))
                      .then();

              return removeMono.then(addMono);
            });
  }

  private static User toDomain(UserEntity entity, List<UUID> preferredIds) {
    if (entity == null) {
      return null;
    }
    Set<UUID> preferred = preferredIds != null ? new LinkedHashSet<>(preferredIds) : Set.of();
    return User.builder()
        .id(entity.getId())
        .pizzeriaId(entity.getPizzeriaId())
        .name(entity.getName())
        .email(entity.getEmail())
        .passwordHash(entity.getPasswordHash())
        .emailVerified(Boolean.TRUE.equals(entity.getEmailVerified()))
        .phone(entity.getPhone())
        .preferredDiet(
            entity.getPreferredDiet() != null ? Diet.valueOf(entity.getPreferredDiet()) : Diet.NONE)
        .preferredIngredientIds(preferred)
        .status(
            entity.getStatus() != null ? UserStatus.valueOf(entity.getStatus()) : UserStatus.ACTIVE)
        .pizzeriaAdmin(entity.getPizzeriaAdmin())
        .profilePhotoBase64(entity.getProfilePhotoBase64())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .createdBy(entity.getCreatedBy())
        .updatedBy(entity.getUpdatedBy())
        .isNew(false)
        .build();
  }

  private static UserEntity toEntity(User user) {
    if (user.isNew()) {
      return UserEntity.builder()
          .id(user.id())
          .pizzeriaId(user.pizzeriaId())
          .name(user.name())
          .email(user.email())
          .passwordHash(user.passwordHash())
          .emailVerified(user.emailVerified())
          .phone(user.phone())
          .preferredDiet((user.preferredDiet() != null ? user.preferredDiet() : Diet.NONE).name())
          .status((user.status() != null ? user.status() : UserStatus.ACTIVE).name())
          .pizzeriaAdmin(user.pizzeriaAdmin())
          .profilePhotoBase64(user.profilePhotoBase64())
          .createdAt(user.createdAt())
          .updatedAt(user.updatedAt())
          .createdBy(user.createdBy())
          .updatedBy(user.updatedBy())
          .isNew(true)
          .build();
    }
    return UserEntity.builder()
        .id(user.id())
        .pizzeriaId(user.pizzeriaId())
        .name(user.name())
        .email(user.email())
        .passwordHash(user.passwordHash())
        .emailVerified(user.emailVerified())
        .phone(user.phone())
        .preferredDiet((user.preferredDiet() != null ? user.preferredDiet() : Diet.NONE).name())
        .status((user.status() != null ? user.status() : UserStatus.ACTIVE).name())
        .pizzeriaAdmin(user.pizzeriaAdmin())
        .profilePhotoBase64(user.profilePhotoBase64())
        .createdAt(user.createdAt())
        .updatedAt(user.updatedAt())
        .createdBy(user.createdBy())
        .updatedBy(user.updatedBy())
        .build();
  }
}
