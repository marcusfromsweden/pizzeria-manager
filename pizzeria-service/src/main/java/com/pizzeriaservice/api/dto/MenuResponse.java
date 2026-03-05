package com.pizzeriaservice.api.dto;

import java.util.List;

public record MenuResponse(
    List<MenuSectionResponse> sections, List<PizzaCustomisationResponse> pizzaCustomisations) {}
