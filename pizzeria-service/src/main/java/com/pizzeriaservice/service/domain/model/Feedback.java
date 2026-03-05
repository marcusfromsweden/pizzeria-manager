package com.pizzeriaservice.service.domain.model;

import com.pizzeriaservice.service.domain.FeedbackKind;
import com.pizzeriaservice.service.domain.FeedbackStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record Feedback(
    UUID id,
    UUID pizzeriaId,
    UUID userId,
    FeedbackKind kind,
    String message,
    Integer rating,
    String category,
    FeedbackStatus status,
    String adminReply,
    Instant adminRepliedAt,
    Instant adminReplyReadAt,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy,
    boolean isNew) {}
