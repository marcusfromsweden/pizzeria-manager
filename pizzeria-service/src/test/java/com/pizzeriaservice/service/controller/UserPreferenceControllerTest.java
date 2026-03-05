package com.pizzeriaservice.service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.api.dto.DietPreferenceUpdateRequest;
import com.pizzeriaservice.api.dto.DietType;
import com.pizzeriaservice.api.dto.PreferredIngredientRequest;
import com.pizzeriaservice.service.service.UserService;
import com.pizzeriaservice.service.support.security.AuthenticatedUser;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class UserPreferenceControllerTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private UserService userService;

  private UserPreferenceController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new UserPreferenceController(userService);
  }

  @Test
  void getDietDelegatesToServiceAndMapsResponse() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);

    when(userService.getDiet(userId, DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(DietType.VEGAN));

    StepVerifier.create(controller.getDiet(user))
        .assertNext(response -> assertThat(response.diet()).isEqualTo(DietType.VEGAN))
        .verifyComplete();

    verify(userService).getDiet(userId, DEFAULT_PIZZERIA_ID);
  }

  @Test
  void updateDietDelegatesToServiceAndMapsResponse() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);
    DietPreferenceUpdateRequest request = new DietPreferenceUpdateRequest(DietType.VEGETARIAN);

    when(userService.updateDiet(userId, DEFAULT_PIZZERIA_ID, DietType.VEGETARIAN))
        .thenReturn(Mono.just(DietType.VEGETARIAN));

    StepVerifier.create(controller.updateDiet(user, request))
        .assertNext(response -> assertThat(response.diet()).isEqualTo(DietType.VEGETARIAN))
        .verifyComplete();

    verify(userService).updateDiet(userId, DEFAULT_PIZZERIA_ID, DietType.VEGETARIAN);
  }

  @Test
  void preferredIngredientsDelegatesToServiceAndMapsResponse() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);
    UUID ingredientId1 = UUID.randomUUID();
    UUID ingredientId2 = UUID.randomUUID();

    when(userService.getPreferredIngredients(userId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(Set.of(ingredientId1, ingredientId2)));

    StepVerifier.create(controller.preferredIngredients(user).collectList())
        .assertNext(
            responses -> {
              assertThat(responses).hasSize(2);
              Set<UUID> ids =
                  Set.of(responses.get(0).ingredientId(), responses.get(1).ingredientId());
              assertThat(ids).containsExactlyInAnyOrder(ingredientId1, ingredientId2);
            })
        .verifyComplete();

    verify(userService).getPreferredIngredients(userId, DEFAULT_PIZZERIA_ID);
  }

  @Test
  void preferredIngredientsReturnsEmptyFluxForNoIngredients() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);

    when(userService.getPreferredIngredients(userId, DEFAULT_PIZZERIA_ID))
        .thenReturn(Mono.just(Set.of()));

    StepVerifier.create(controller.preferredIngredients(user).collectList())
        .assertNext(responses -> assertThat(responses).isEmpty())
        .verifyComplete();

    verify(userService).getPreferredIngredients(userId, DEFAULT_PIZZERIA_ID);
  }

  @Test
  void addPreferredIngredientDelegatesToServiceAndReturns201() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);
    UUID ingredientId = UUID.randomUUID();
    PreferredIngredientRequest request = new PreferredIngredientRequest(ingredientId);

    when(userService.addPreferredIngredient(userId, DEFAULT_PIZZERIA_ID, ingredientId))
        .thenReturn(Mono.empty());

    StepVerifier.create(controller.addPreferredIngredient(user, request)).verifyComplete();

    verify(userService).addPreferredIngredient(userId, DEFAULT_PIZZERIA_ID, ingredientId);
  }

  @Test
  void removePreferredIngredientDelegatesToServiceAndReturnsNoContent() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser user = new AuthenticatedUser(userId, DEFAULT_PIZZERIA_ID, null);
    UUID ingredientId = UUID.randomUUID();

    when(userService.removePreferredIngredient(userId, DEFAULT_PIZZERIA_ID, ingredientId))
        .thenReturn(Mono.empty());

    StepVerifier.create(controller.removePreferredIngredient(user, ingredientId)).verifyComplete();

    verify(userService).removePreferredIngredient(userId, DEFAULT_PIZZERIA_ID, ingredientId);
  }
}
