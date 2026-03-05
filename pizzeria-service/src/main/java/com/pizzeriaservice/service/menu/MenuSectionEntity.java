package com.pizzeriaservice.service.menu;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("menu_sections")
public record MenuSectionEntity(
    @Id UUID id,
    @Column("pizzeria_id") UUID pizzeriaId,
    String code,
    @Column("translation_key") String translationKey,
    @Column("sort_order") Integer sortOrder,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt) {}
