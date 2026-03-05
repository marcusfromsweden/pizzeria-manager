package com.pizzeriaservice.service.menu;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("pizza_customisations")
public record PizzaCustomisationEntity(
    @Id UUID id,
    @Column("pizzeria_id") UUID pizzeriaId,
    @Column("name_key") String nameKey,
    @Column("price_regular") BigDecimal priceRegular,
    @Column("price_family") BigDecimal priceFamily,
    @Column("sort_order") Integer sortOrder,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt) {}
