package com.pizzeriaservice.service.menu;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("menu_items")
public record MenuItemEntity(
    @Id UUID id,
    @Column("pizzeria_id") UUID pizzeriaId,
    @Column("section_id") UUID sectionId,
    @Column("dish_number") Integer dishNumber,
    @Column("name_key") String nameKey,
    @Column("description_key") String descriptionKey,
    @Column("price_regular") java.math.BigDecimal priceRegular,
    @Column("price_family") java.math.BigDecimal priceFamily,
    @Column("sort_order") Integer sortOrder,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt) {}
