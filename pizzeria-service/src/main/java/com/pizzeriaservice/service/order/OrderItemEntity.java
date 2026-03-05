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

@Table("order_items")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderItemEntity implements Persistable<UUID> {

  @Id private UUID id;

  @Column("pizzeria_id")
  private UUID pizzeriaId;

  @Column("order_id")
  private UUID orderId;

  @Column("menu_item_id")
  private UUID menuItemId;

  @Column("menu_item_name_key")
  private String menuItemNameKey;

  private String size;
  private int quantity;

  @Column("base_price")
  private BigDecimal basePrice;

  @Column("customisations_price")
  private BigDecimal customisationsPrice;

  @Column("item_total")
  private BigDecimal itemTotal;

  @Column("special_instructions")
  private String specialInstructions;

  @Column("created_at")
  private Instant createdAt;

  @Column("created_by")
  private UUID createdBy;

  @Transient @Builder.Default private boolean isNew = false;
}
