package com.pizzeriaservice.service.service;

import com.pizzeriaservice.api.dto.MenuIngredientResponse;
import com.pizzeriaservice.api.dto.PizzaDetailResponse;
import com.pizzeriaservice.api.dto.PizzaSuitabilityRequest;
import com.pizzeriaservice.api.dto.PizzaSuitabilityResponse;
import com.pizzeriaservice.api.dto.PizzaSummaryResponse;
import com.pizzeriaservice.service.domain.Diet;
import com.pizzeriaservice.service.domain.PizzaSize;
import com.pizzeriaservice.service.domain.model.User;
import com.pizzeriaservice.service.menu.MenuIngredientFactEntity;
import com.pizzeriaservice.service.menu.MenuIngredientFactRepository;
import com.pizzeriaservice.service.menu.MenuItemEntity;
import com.pizzeriaservice.service.menu.MenuItemIngredientEntity;
import com.pizzeriaservice.service.menu.MenuItemIngredientRepository;
import com.pizzeriaservice.service.menu.MenuItemRepository;
import com.pizzeriaservice.service.menu.MenuSectionRepository;
import com.pizzeriaservice.service.menu.PizzaCustomisationRepository;
import com.pizzeriaservice.service.repository.UserRepository;
import com.pizzeriaservice.service.support.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PizzaService {

  private final MenuSectionRepository sectionRepository;
  private final MenuItemRepository itemRepository;
  private final MenuItemIngredientRepository ingredientRepository;
  private final MenuIngredientFactRepository ingredientFactRepository;
  private final PizzaCustomisationRepository customisationRepository;
  private final UserRepository userRepository;

  public Flux<PizzaSummaryResponse> list(UUID pizzeriaId) {
    return sectionRepository
        .findAllByPizzeriaIdOrderBySortOrderAsc(pizzeriaId)
        .filter(section -> section.code().contains("pizza"))
        .flatMap(section -> itemRepository.findAllBySectionIdOrderBySortOrderAsc(section.id()))
        .flatMap(entity -> toSummaryResponseWithDietaryType(entity, pizzeriaId));
  }

  public Mono<PizzaDetailResponse> get(UUID pizzaId, UUID pizzeriaId) {
    return itemRepository
        .findByIdAndPizzeriaId(pizzaId, pizzeriaId)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Pizza", pizzaId)))
        .flatMap(entity -> mapToDetailResponse(entity, pizzeriaId));
  }

  public Mono<PizzaSuitabilityResponse> suitability(
      PizzaSuitabilityRequest request, UUID userId, UUID pizzeriaId) {
    Mono<User> userMono =
        userRepository
            .findByIdAndPizzeriaId(userId, pizzeriaId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", userId)))
            .cache();

    Mono<MenuItemEntity> pizzaMono =
        itemRepository
            .findByIdAndPizzeriaId(request.pizzaId(), pizzeriaId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Pizza", request.pizzaId())));

    Mono<List<String>> pizzaIngredientKeysMono =
        ingredientRepository
            .findAllByMenuItemIdOrderBySortOrderAsc(request.pizzaId())
            .map(MenuItemIngredientEntity::ingredientKey)
            .collectList()
            .cache();

    Mono<Map<String, MenuIngredientFactEntity>> pizzaFactsMono =
        pizzaIngredientKeysMono.flatMap(keys -> fetchFactsByKeys(keys, pizzeriaId)).cache();

    Mono<Map<UUID, MenuIngredientFactEntity>> additionFactsMono =
        fetchFactsByIds(request.additionalIngredientIds(), pizzeriaId);
    Mono<Map<UUID, MenuIngredientFactEntity>> removalFactsMono =
        fetchFactsByIds(request.removedIngredientIds(), pizzeriaId);
    Mono<Map<UUID, MenuIngredientFactEntity>> preferredFactsMono =
        userMono
            .map(User::preferredIngredientIds)
            .flatMap(
                ids ->
                    fetchFactsByIds(
                        Optional.ofNullable(ids).map(List::copyOf).orElse(List.of()), pizzeriaId));

    return Mono.zip(
            userMono,
            pizzaMono,
            pizzaIngredientKeysMono,
            pizzaFactsMono,
            additionFactsMono,
            removalFactsMono,
            preferredFactsMono)
        .map(
            tuple ->
                buildSuitabilityResponse(
                    tuple.getT1(),
                    tuple.getT3(),
                    tuple.getT4(),
                    tuple.getT5(),
                    tuple.getT6(),
                    tuple.getT7()));
  }

  private PizzaSuitabilityResponse buildSuitabilityResponse(
      User user,
      List<String> pizzaIngredientKeys,
      Map<String, MenuIngredientFactEntity> pizzaFacts,
      Map<UUID, MenuIngredientFactEntity> additionFacts,
      Map<UUID, MenuIngredientFactEntity> removalFacts,
      Map<UUID, MenuIngredientFactEntity> preferredFacts) {

    Map<String, MenuIngredientFactEntity> finalFacts = new LinkedHashMap<>(pizzaFacts);

    Optional.ofNullable(removalFacts)
        .ifPresent(
            facts ->
                facts.values().stream()
                    .filter(Objects::nonNull)
                    .forEach(fact -> finalFacts.remove(fact.ingredientKey())));

    Optional.ofNullable(additionFacts)
        .ifPresent(
            facts ->
                facts.values().stream()
                    .filter(Objects::nonNull)
                    .forEach(fact -> finalFacts.put(fact.ingredientKey(), fact)));

    Diet diet = Optional.ofNullable(user.preferredDiet()).orElse(Diet.NONE);
    List<String> violations = new ArrayList<>();
    finalFacts
        .values()
        .forEach(
            fact -> {
              if (fact != null && !isIngredientAllowedForDiet(fact, diet)) {
                violations.add(
                    "Ingredient %s violates %s diet"
                        .formatted(fact.ingredientKey(), diet.name().toLowerCase()));
              }
            });

    List<String> suggestions = new ArrayList<>();
    Optional.ofNullable(preferredFacts)
        .ifPresent(
            facts ->
                facts.values().stream()
                    .filter(Objects::nonNull)
                    .filter(fact -> !finalFacts.containsKey(fact.ingredientKey()))
                    .forEach(
                        fact ->
                            suggestions.add(
                                "Add preferred ingredient %s".formatted(fact.ingredientKey()))));

    boolean suitable = violations.isEmpty();
    return new PizzaSuitabilityResponse(
        suitable, List.copyOf(violations), List.copyOf(suggestions));
  }

  private boolean isIngredientAllowedForDiet(MenuIngredientFactEntity fact, Diet diet) {
    if (diet == Diet.NONE || fact == null || fact.dietaryType() == null) {
      return true;
    }
    String type = fact.dietaryType().toUpperCase();
    return switch (diet) {
      case VEGAN -> type.equals("VEGAN");
      case VEGETARIAN -> type.equals("VEGAN") || type.equals("VEGETARIAN");
      case CARNIVORE -> true;
      case NONE -> true;
    };
  }

  private Mono<Map<String, MenuIngredientFactEntity>> fetchFactsByKeys(
      List<String> keys, UUID pizzeriaId) {
    if (keys == null || keys.isEmpty()) {
      return Mono.just(new LinkedHashMap<>());
    }
    return Flux.fromIterable(new LinkedHashSet<>(keys))
        .flatMap(
            key ->
                ingredientFactRepository
                    .findByIngredientKeyAndPizzeriaId(key, pizzeriaId)
                    .defaultIfEmpty(
                        new MenuIngredientFactEntity(
                            null, pizzeriaId, key, "UNKNOWN", "", 0, java.math.BigDecimal.ZERO)))
        .collectMap(MenuIngredientFactEntity::ingredientKey, fact -> fact, LinkedHashMap::new);
  }

  private Mono<Map<UUID, MenuIngredientFactEntity>> fetchFactsByIds(
      List<UUID> ids, UUID pizzeriaId) {
    if (ids == null || ids.isEmpty()) {
      return Mono.just(Map.of());
    }
    return ingredientFactRepository
        .findAllByIdInAndPizzeriaId(ids, pizzeriaId)
        .collectMap(MenuIngredientFactEntity::id, fact -> fact);
  }

  private Mono<PizzaSummaryResponse> toSummaryResponseWithDietaryType(
      MenuItemEntity entity, UUID pizzeriaId) {
    return ingredientRepository
        .findAllByMenuItemIdOrderBySortOrderAsc(entity.id())
        .flatMap(
            ingredientEntry ->
                ingredientFactRepository
                    .findByIngredientKeyAndPizzeriaId(ingredientEntry.ingredientKey(), pizzeriaId)
                    .defaultIfEmpty(
                        new MenuIngredientFactEntity(
                            null,
                            pizzeriaId,
                            ingredientEntry.ingredientKey(),
                            "UNKNOWN",
                            "",
                            0,
                            java.math.BigDecimal.ZERO))
                    .map(
                        fact ->
                            new MenuIngredientResponse(
                                fact.id(),
                                ingredientEntry.ingredientKey(),
                                fact.dietaryType(),
                                parseAllergens(fact.allergenTags()),
                                Optional.ofNullable(fact.spiceLevel()).orElse(0),
                                fact.caloriesPer100g())))
        .collectList()
        .map(
            ingredients ->
                new PizzaSummaryResponse(
                    entity.id(),
                    entity.dishNumber(),
                    entity.nameKey(),
                    entity.priceRegular(),
                    entity.priceFamily(),
                    computeOverallDietaryType(ingredients),
                    Optional.ofNullable(entity.sortOrder()).orElse(0),
                    entity.totalCalories()));
  }

  private Mono<PizzaDetailResponse> mapToDetailResponse(MenuItemEntity entity, UUID pizzeriaId) {
    return ingredientRepository
        .findAllByMenuItemIdOrderBySortOrderAsc(entity.id())
        .flatMap(
            ingredientEntry ->
                ingredientFactRepository
                    .findByIngredientKeyAndPizzeriaId(ingredientEntry.ingredientKey(), pizzeriaId)
                    .defaultIfEmpty(
                        new MenuIngredientFactEntity(
                            null,
                            pizzeriaId,
                            ingredientEntry.ingredientKey(),
                            "UNKNOWN",
                            "",
                            0,
                            java.math.BigDecimal.ZERO))
                    .map(
                        fact ->
                            new MenuIngredientResponse(
                                fact.id(),
                                ingredientEntry.ingredientKey(),
                                fact.dietaryType(),
                                parseAllergens(fact.allergenTags()),
                                Optional.ofNullable(fact.spiceLevel()).orElse(0),
                                fact.caloriesPer100g())))
        .collectList()
        .map(
            ingredients ->
                new PizzaDetailResponse(
                    entity.id(),
                    entity.dishNumber(),
                    entity.nameKey(),
                    entity.descriptionKey(),
                    entity.priceRegular(),
                    entity.priceFamily(),
                    ingredients,
                    computeOverallDietaryType(ingredients),
                    Optional.ofNullable(entity.sortOrder()).orElse(0),
                    entity.totalCalories()));
  }

  private String computeOverallDietaryType(List<MenuIngredientResponse> ingredients) {
    if (ingredients.isEmpty()) {
      return "UNKNOWN";
    }

    boolean hasCarnivore = ingredients.stream().anyMatch(i -> "CARNIVORE".equals(i.dietaryType()));
    if (hasCarnivore) {
      return "CARNIVORE";
    }

    boolean hasVegetarian =
        ingredients.stream().anyMatch(i -> "VEGETARIAN".equals(i.dietaryType()));
    if (hasVegetarian) {
      return "VEGETARIAN";
    }

    boolean allVegan = ingredients.stream().allMatch(i -> "VEGAN".equals(i.dietaryType()));
    if (allVegan) {
      return "VEGAN";
    }

    return "UNKNOWN";
  }

  private static java.util.Set<String> parseAllergens(String tags) {
    if (tags == null || tags.isBlank()) {
      return java.util.Set.of();
    }
    return java.util.Arrays.stream(tags.split(","))
        .map(String::trim)
        .filter(tag -> !tag.isEmpty())
        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
  }

  public Mono<BigDecimal> priceForCustomisations(
      List<UUID> customisationIds, UUID pizzeriaId, PizzaSize size) {
    if (customisationIds == null || customisationIds.isEmpty()) {
      return Mono.just(BigDecimal.ZERO);
    }
    return customisationRepository
        .findAllByPizzeriaIdOrderBySortOrderAsc(pizzeriaId)
        .filter(entity -> customisationIds.contains(entity.id()))
        .map(
            entity -> {
              if (size == PizzaSize.FAMILY && entity.priceFamily() != null) {
                return entity.priceFamily();
              }
              return entity.priceRegular();
            })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
