package com.pizzeriaservice.api.dto;

import java.util.List;
import java.util.UUID;

public record MenuSectionResponse(
    UUID id, String code, String translationKey, int sortOrder, List<MenuItemResponse> items) {}
