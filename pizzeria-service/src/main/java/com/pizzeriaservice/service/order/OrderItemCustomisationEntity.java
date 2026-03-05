package com.pizzeriaservice.service.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("order_item_customisations")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderItemCustomisationEntity implements Persistable<UUID> {

  @Id private UUID id;

  @Column("pizzeria_id")
  private UUID pizzeriaId;

  @Column("order_item_id")
  private UUID orderItemId;

  @Column("customisation_id")
  private UUID customisationId;

  @Column("customisation_name_key")
  private String customisationNameKey;

  private BigDecimal price;

  @Column("created_at")
  private Instant createdAt;

  @Transient @Builder.Default private boolean isNew = false;
}
