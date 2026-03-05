package com.pizzeriaservice.service.user;

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

@Table("users")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserEntity implements Persistable<UUID> {

  @Id private UUID id;

  @Column("pizzeria_id")
  private UUID pizzeriaId;

  private String name;
  private String email;

  @Column("password_hash")
  private String passwordHash;

  @Column("email_verified")
  private Boolean emailVerified;

  private String phone;

  @Column("preferred_diet")
  private String preferredDiet;

  private String status;

  @Column("pizzeria_admin")
  private String pizzeriaAdmin;

  @Column("profile_photo_base64")
  private String profilePhotoBase64;

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
