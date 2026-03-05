package com.pizzeriaservice.service.repository.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.service.domain.Diet;
import com.pizzeriaservice.service.domain.UserStatus;
import com.pizzeriaservice.service.domain.model.User;
import com.pizzeriaservice.service.user.UserEntity;
import com.pizzeriaservice.service.user.UserPreferredIngredientEntity;
import com.pizzeriaservice.service.user.UserPreferredIngredientRepository;
import com.pizzeriaservice.service.user.UserRepositoryR2dbc;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class UserRepositoryAdapterTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private UserRepositoryR2dbc userRepository;
  @Mock private UserPreferredIngredientRepository preferredRepository;

  private UserRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    adapter = new UserRepositoryAdapter(userRepository, preferredRepository);
  }

  @Test
  void saveSyncsPreferredIngredients() {
    UUID userId = UUID.randomUUID();
    UUID ingredientKeep = UUID.randomUUID();
    UUID ingredientAdd = UUID.randomUUID();
    UUID ingredientRemove = UUID.randomUUID();
    Instant now = Instant.parse("2024-04-04T09:00:00Z");

    User user =
        User.builder()
            .id(userId)
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("Sam")
            .email("sam@example.com")
            .passwordHash("hash")
            .emailVerified(true)
            .preferredDiet(Diet.VEGAN)
            .preferredIngredientIds(new LinkedHashSet<>(Set.of(ingredientKeep, ingredientAdd)))
            .status(UserStatus.ACTIVE)
            .createdAt(now.minusSeconds(60))
            .updatedAt(now)
            .build();

    UserEntity entity =
        new UserEntity(
            userId,
            DEFAULT_PIZZERIA_ID,
            "Sam",
            "sam@example.com",
            "hash",
            true,
            null,
            "VEGAN",
            "ACTIVE",
            null,
            null,
            now.minusSeconds(60),
            now,
            null,
            null,
            false);

    UserPreferredIngredientEntity keepEntity =
        new UserPreferredIngredientEntity(
            UUID.randomUUID(), DEFAULT_PIZZERIA_ID, userId, ingredientKeep, now, false);
    UserPreferredIngredientEntity removeEntity =
        new UserPreferredIngredientEntity(
            UUID.randomUUID(), DEFAULT_PIZZERIA_ID, userId, ingredientRemove, now, false);
    UserPreferredIngredientEntity addedEntity =
        new UserPreferredIngredientEntity(
            UUID.randomUUID(), DEFAULT_PIZZERIA_ID, userId, ingredientAdd, now, false);

    when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(entity));
    when(userRepository.findById(userId)).thenReturn(Mono.just(entity));
    when(preferredRepository.findAllByUserId(userId))
        .thenReturn(Flux.just(keepEntity, removeEntity))
        .thenReturn(Flux.just(keepEntity, addedEntity));
    when(preferredRepository.deleteByUserIdAndIngredientId(userId, ingredientRemove))
        .thenReturn(Mono.just(1L));
    when(preferredRepository.save(any(UserPreferredIngredientEntity.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(adapter.save(user))
        .assertNext(
            saved ->
                assertThat(saved.preferredIngredientIds())
                    .containsExactlyInAnyOrder(ingredientKeep, ingredientAdd))
        .verifyComplete();

    verify(preferredRepository).deleteByUserIdAndIngredientId(userId, ingredientRemove);
    ArgumentCaptor<UserPreferredIngredientEntity> entityCaptor =
        ArgumentCaptor.forClass(UserPreferredIngredientEntity.class);
    verify(preferredRepository).save(entityCaptor.capture());
    assertThat(entityCaptor.getValue().getIngredientId()).isEqualTo(ingredientAdd);

    verify(preferredRepository, times(2)).findAllByUserId(userId);
  }

  @Test
  void saveHandlesNullPreferredIds() {
    UUID userId = UUID.randomUUID();
    Instant now = Instant.parse("2024-04-04T09:00:00Z");

    User user =
        User.builder()
            .id(userId)
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .name("No Prefs")
            .email("noprefs@example.com")
            .passwordHash("hash")
            .emailVerified(true)
            .preferredDiet(Diet.NONE)
            .preferredIngredientIds(null)
            .status(UserStatus.ACTIVE)
            .createdAt(now.minusSeconds(60))
            .updatedAt(now)
            .build();

    UserEntity entity =
        new UserEntity(
            userId,
            DEFAULT_PIZZERIA_ID,
            "No Prefs",
            "noprefs@example.com",
            "hash",
            true,
            null,
            "NONE",
            "ACTIVE",
            null,
            null,
            now.minusSeconds(60),
            now,
            null,
            null,
            false);

    when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(entity));
    when(userRepository.findById(userId)).thenReturn(Mono.just(entity));
    when(preferredRepository.findAllByUserId(userId)).thenReturn(Flux.empty());

    StepVerifier.create(adapter.save(user))
        .assertNext(saved -> assertThat(saved.preferredIngredientIds()).isEmpty())
        .verifyComplete();

    verify(preferredRepository, times(2)).findAllByUserId(userId);
    verify(preferredRepository, times(0)).deleteByUserIdAndIngredientId(any(), any());
    verify(preferredRepository, times(0)).save(any());
  }

  @Test
  void deleteByIdRemovesPreferredIngredientsFirst() {
    UUID userId = UUID.randomUUID();
    when(preferredRepository.deleteByUserId(userId)).thenReturn(Mono.just(2L));
    when(userRepository.deleteById(userId)).thenReturn(Mono.empty());

    StepVerifier.create(adapter.deleteById(userId)).verifyComplete();

    verify(preferredRepository).deleteByUserId(userId);
    verify(userRepository).deleteById(userId);
  }

  @Test
  void findByIdAndPizzeriaIdReturnsUserWithPreferredIngredients() {
    UUID userId = UUID.randomUUID();
    UUID ingredientId = UUID.randomUUID();
    UserEntity entity =
        new UserEntity(
            userId,
            DEFAULT_PIZZERIA_ID,
            "Jane",
            "jane@example.com",
            "hash",
            true,
            null,
            "VEGETARIAN",
            "ACTIVE",
            null,
            null,
            Instant.parse("2024-04-04T09:00:00Z"),
            Instant.parse("2024-04-04T09:01:00Z"),
            null,
            null,
            false);
    UserPreferredIngredientEntity prefEntity =
        new UserPreferredIngredientEntity(
            UUID.randomUUID(), DEFAULT_PIZZERIA_ID, userId, ingredientId, Instant.now(), false);

    when(userRepository.findByIdAndPizzeriaId(userId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(entity));
    when(userRepository.findById(userId)).thenReturn(Mono.just(entity));
    when(preferredRepository.findAllByUserId(userId)).thenReturn(Flux.just(prefEntity));

    StepVerifier.create(adapter.findByIdAndPizzeriaId(userId, DEFAULT_PIZZERIA_ID))
        .assertNext(
            user -> {
              assertThat(user.id()).isEqualTo(userId);
              assertThat(user.preferredIngredientIds()).containsExactly(ingredientId);
              assertThat(user.preferredDiet()).isEqualTo(Diet.VEGETARIAN);
            })
        .verifyComplete();
  }

  @Test
  void findByEmailAndPizzeriaIdReturnsUserWithoutPreferences() {
    UUID userId = UUID.randomUUID();
    String email = "noprefs@example.com";
    UserEntity entity =
        new UserEntity(
            userId,
            DEFAULT_PIZZERIA_ID,
            "Alex",
            email,
            "hash",
            false,
            null,
            "NONE",
            "ACTIVE",
            null,
            null,
            Instant.parse("2024-04-04T09:00:00Z"),
            Instant.parse("2024-04-04T09:01:00Z"),
            null,
            null,
            false);

    when(userRepository.findByEmailAndPizzeriaId(email, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(entity));
    when(userRepository.findById(userId)).thenReturn(Mono.just(entity));
    when(preferredRepository.findAllByUserId(userId)).thenReturn(Flux.empty());

    StepVerifier.create(adapter.findByEmailAndPizzeriaId(email, DEFAULT_PIZZERIA_ID))
        .assertNext(
            user -> {
              assertThat(user.email()).isEqualTo(email);
              assertThat(user.preferredIngredientIds()).isEmpty();
              assertThat(user.preferredDiet()).isEqualTo(Diet.NONE);
            })
        .verifyComplete();
  }

  @Test
  void findAllByPizzeriaIdAggregatesPreferences() {
    UUID userOneId = UUID.randomUUID();
    UUID userTwoId = UUID.randomUUID();
    UUID prefOne = UUID.randomUUID();
    UUID prefTwo = UUID.randomUUID();

    UserEntity userOne =
        new UserEntity(
            userOneId,
            DEFAULT_PIZZERIA_ID,
            "User One",
            "one@example.com",
            "hash",
            true,
            null,
            "NONE",
            "ACTIVE",
            null,
            null,
            Instant.parse("2024-04-04T09:00:00Z"),
            Instant.parse("2024-04-04T09:01:00Z"),
            null,
            null,
            false);
    UserEntity userTwo =
        new UserEntity(
            userTwoId,
            DEFAULT_PIZZERIA_ID,
            "User Two",
            "two@example.com",
            "hash",
            true,
            null,
            "VEGAN",
            "ACTIVE",
            null,
            null,
            Instant.parse("2024-04-04T09:00:00Z"),
            Instant.parse("2024-04-04T09:01:00Z"),
            null,
            null,
            false);

    when(userRepository.findAllByPizzeriaId(DEFAULT_PIZZERIA_ID))
        .thenReturn(Flux.just(userOne, userTwo));
    when(preferredRepository.findAllByUserId(userOneId))
        .thenReturn(
            Flux.just(
                new UserPreferredIngredientEntity(
                    UUID.randomUUID(),
                    DEFAULT_PIZZERIA_ID,
                    userOneId,
                    prefOne,
                    Instant.now(),
                    false)));
    when(preferredRepository.findAllByUserId(userTwoId))
        .thenReturn(
            Flux.just(
                new UserPreferredIngredientEntity(
                    UUID.randomUUID(),
                    DEFAULT_PIZZERIA_ID,
                    userTwoId,
                    prefTwo,
                    Instant.now(),
                    false)));

    StepVerifier.create(adapter.findAllByPizzeriaId(DEFAULT_PIZZERIA_ID).collectList())
        .assertNext(
            users -> {
              assertThat(users).hasSize(2);
              assertThat(users.get(0).preferredIngredientIds()).containsExactly(prefOne);
              assertThat(users.get(1).preferredIngredientIds()).containsExactly(prefTwo);
            })
        .verifyComplete();
  }
}
