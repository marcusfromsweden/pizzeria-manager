package com.pizzeriaservice.service.service;

import com.pizzeriaservice.api.dto.MenuIngredientResponse;
import com.pizzeriaservice.api.dto.MenuItemResponse;
import com.pizzeriaservice.api.dto.MenuResponse;
import com.pizzeriaservice.api.dto.MenuSectionResponse;
import com.pizzeriaservice.api.dto.PizzaCustomisationResponse;
import com.pizzeriaservice.service.menu.MenuIngredientFactEntity;
import com.pizzeriaservice.service.menu.MenuIngredientFactRepository;
import com.pizzeriaservice.service.menu.MenuItemEntity;
import com.pizzeriaservice.service.menu.MenuItemIngredientRepository;
import com.pizzeriaservice.service.menu.MenuItemRepository;
import com.pizzeriaservice.service.menu.MenuSectionEntity;
import com.pizzeriaservice.service.menu.MenuSectionRepository;
import com.pizzeriaservice.service.menu.PizzaCustomisationEntity;
import com.pizzeriaservice.service.menu.PizzaCustomisationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MenuService {

  private final MenuSectionRepository sectionRepository;
  private final MenuItemRepository itemRepository;
  private final MenuItemIngredientRepository ingredientRepository;
  private final MenuIngredientFactRepository ingredientFactRepository;
  private final PizzaCustomisationRepository customisationRepository;

  public Mono<MenuResponse> getMenu(UUID pizzeriaId) {
    Mono<List<MenuSectionResponse>> sectionsMono =
        sectionRepository
            .findAllByPizzeriaIdOrderBySortOrderAsc(pizzeriaId)
            .flatMap(section -> mapSectionWithItems(section, pizzeriaId))
            .collectList();

    Mono<List<PizzaCustomisationResponse>> customisationsMono =
        customisationRepository
            .findAllByPizzeriaIdOrderBySortOrderAsc(pizzeriaId)
            .map(this::toCustomisationResponse)
            .collectList();

    return Mono.zip(sectionsMono, customisationsMono)
        .map(tuple -> new MenuResponse(tuple.getT1(), tuple.getT2()));
  }

  private Mono<MenuSectionResponse> mapSectionWithItems(
      MenuSectionEntity section, UUID pizzeriaId) {
    return itemRepository
        .findAllBySectionIdOrderBySortOrderAsc(section.id())
        .flatMap(item -> mapItemWithIngredients(item, pizzeriaId))
        .collectList()
        .map(
            items ->
                new MenuSectionResponse(
                    section.id(),
                    section.code(),
                    section.translationKey(),
                    section.sortOrder() != null ? section.sortOrder() : 0,
                    items));
  }

  private Mono<MenuItemResponse> mapItemWithIngredients(MenuItemEntity item, UUID pizzeriaId) {
    return ingredientRepository
        .findAllByMenuItemIdOrderBySortOrderAsc(item.id())
        .flatMap(
            entry ->
                ingredientFactRepository
                    .findByIngredientKeyAndPizzeriaId(entry.ingredientKey(), pizzeriaId)
                    .defaultIfEmpty(
                        new MenuIngredientFactEntity(
                            null, null, entry.ingredientKey(), "UNKNOWN", "", 0))
                    .map(
                        fact ->
                            new MenuIngredientResponse(
                                fact.id(),
                                entry.ingredientKey(),
                                fact.dietaryType(),
                                parseAllergens(fact.allergenTags()),
                                fact.spiceLevel() != null ? fact.spiceLevel() : 0)))
        .collectList()
        .map(ingredients -> toMenuItemResponse(item, ingredients));
  }

  private MenuItemResponse toMenuItemResponse(
      MenuItemEntity item, List<MenuIngredientResponse> ingredients) {
    return new MenuItemResponse(
        item.id(),
        item.sectionId(),
        item.dishNumber(),
        item.nameKey(),
        item.descriptionKey(),
        item.priceRegular(),
        item.priceFamily(),
        ingredients,
        computeOverallDietaryType(ingredients),
        item.sortOrder() != null ? item.sortOrder() : 0);
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

  private PizzaCustomisationResponse toCustomisationResponse(PizzaCustomisationEntity entity) {
    return new PizzaCustomisationResponse(
        entity.id(),
        entity.nameKey(),
        entity.priceRegular(),
        entity.priceFamily(),
        entity.sortOrder() != null ? entity.sortOrder() : 0);
  }

  public Mono<MenuItemResponse> getMenuItem(UUID id, UUID pizzeriaId) {
    return itemRepository
        .findByIdAndPizzeriaId(id, pizzeriaId)
        .flatMap(item -> mapItemWithIngredients(item, pizzeriaId));
  }

  public Flux<PizzaCustomisationResponse> getPizzaCustomisations(UUID pizzeriaId) {
    return customisationRepository
        .findAllByPizzeriaIdOrderBySortOrderAsc(pizzeriaId)
        .map(this::toCustomisationResponse);
  }
}
