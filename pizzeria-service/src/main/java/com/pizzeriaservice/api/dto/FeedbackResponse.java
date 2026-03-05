package com.pizzeriaservice.api.dto;

import java.time.Instant;
import java.util.UUID;

public record FeedbackResponse(
    UUID id,
    UUID userId,
    String type,
    String message,
    Integer rating,
    String category,
    String adminReply,
    Instant adminRepliedAt,
    Instant adminReplyReadAt,
    Instant createdAt) {}
