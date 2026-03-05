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

@Table("orders")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OrderEntity implements Persistable<UUID> {

  @Id private UUID id;

  @Column("pizzeria_id")
  private UUID pizzeriaId;

  @Column("user_id")
  private UUID userId;

  @Column("order_number")
  private String orderNumber;

  private String status;

  @Column("fulfillment_type")
  private String fulfillmentType;

  @Column("delivery_address_id")
  private UUID deliveryAddressId;

  @Column("delivery_street")
  private String deliveryStreet;

  @Column("delivery_postal_code")
  private String deliveryPostalCode;

  @Column("delivery_city")
  private String deliveryCity;

  @Column("delivery_phone")
  private String deliveryPhone;

  @Column("delivery_instructions")
  private String deliveryInstructions;

  @Column("requested_time")
  private Instant requestedTime;

  @Column("estimated_ready_time")
  private Instant estimatedReadyTime;

  private BigDecimal subtotal;

  @Column("delivery_fee")
  private BigDecimal deliveryFee;

  private BigDecimal total;

  @Column("customer_notes")
  private String customerNotes;

  @Column("created_at")
  private Instant createdAt;

  @Column("updated_at")
  private Instant updatedAt;

  @Column("created_by")
  private UUID createdBy;

  @Column("updated_by")
  private UUID updatedBy;

  @Transient @Builder.Default private boolean isNew = false;
}
