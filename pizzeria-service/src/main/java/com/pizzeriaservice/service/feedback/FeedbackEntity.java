package com.pizzeriaservice.service.feedback;

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

@Table("feedback")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FeedbackEntity implements Persistable<UUID> {

  @Id private UUID id;

  @Column("pizzeria_id")
  private UUID pizzeriaId;

  @Column("user_id")
  private UUID userId;

  private String kind;
  private String message;
  private Integer rating;
  private String category;
  private String status;

  @Column("admin_reply")
  private String adminReply;

  @Column("admin_replied_at")
  private Instant adminRepliedAt;

  @Column("admin_reply_read_at")
  private Instant adminReplyReadAt;

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
