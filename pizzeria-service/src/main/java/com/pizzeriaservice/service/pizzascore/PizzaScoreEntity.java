package com.pizzeriaservice.service.pizzascore;

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

@Table("pizza_scores")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PizzaScoreEntity implements Persistable<UUID> {

  @Id private UUID id;

  @Column("pizzeria_id")
  private UUID pizzeriaId;

  @Column("user_id")
  private UUID userId;

  @Column("pizza_id")
  private UUID pizzaId;

  @Column("pizza_kind")
  private String pizzaKind;

  private Integer score;
  private String comment;

  @Column("created_at")
  private Instant createdAt;

  @Column("created_by")
  private UUID createdBy;

  @Transient @Builder.Default private boolean isNew = false;
}
