package com.pizzeriaservice.service.pizzeria;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("pizzerias")
public record PizzeriaEntity(
    @Id UUID id,
    String code,
    String name,
    String currency,
    String timezone,
    String config,
    String branding,
    Boolean active,
    @Column("created_at") Instant createdAt,
    @Column("updated_at") Instant updatedAt) {}
