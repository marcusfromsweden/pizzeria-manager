package com.pizzeriaservice.service.repository.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pizzeriaservice.service.domain.FeedbackKind;
import com.pizzeriaservice.service.domain.FeedbackStatus;
import com.pizzeriaservice.service.domain.model.Feedback;
import com.pizzeriaservice.service.feedback.FeedbackEntity;
import com.pizzeriaservice.service.feedback.FeedbackRepositoryR2dbc;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class FeedbackRepositoryAdapterTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock private FeedbackRepositoryR2dbc r2dbcRepository;
  private FeedbackRepositoryAdapter adapter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    adapter = new FeedbackRepositoryAdapter(r2dbcRepository);
  }

  @Test
  void saveMapsDomainToEntityAndBack() {
    UUID feedbackId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Instant now = Instant.parse("2024-02-02T10:15:30Z");
    Feedback feedback =
        Feedback.builder()
            .id(feedbackId)
            .pizzeriaId(DEFAULT_PIZZERIA_ID)
            .userId(userId)
            .kind(FeedbackKind.SERVICE)
            .message("Great experience")
            .rating(5)
            .status(FeedbackStatus.OPEN)
            .createdAt(now)
            .updatedAt(now)
            .build();

    FeedbackEntity persistedEntity =
        new FeedbackEntity(
            feedbackId,
            DEFAULT_PIZZERIA_ID,
            userId,
            "SERVICE",
            "Great experience",
            5,
            null,
            "OPEN",
            null,
            null,
            null,
            now,
            now,
            null,
            null,
            false);
    when(r2dbcRepository.save(any(FeedbackEntity.class))).thenReturn(Mono.just(persistedEntity));

    StepVerifier.create(adapter.save(feedback))
        .assertNext(
            saved -> {
              assertThat(saved.id()).isEqualTo(feedbackId);
              assertThat(saved.pizzeriaId()).isEqualTo(DEFAULT_PIZZERIA_ID);
              assertThat(saved.userId()).isEqualTo(userId);
              assertThat(saved.kind()).isEqualTo(FeedbackKind.SERVICE);
              assertThat(saved.status()).isEqualTo(FeedbackStatus.OPEN);
              assertThat(saved.message()).isEqualTo("Great experience");
              assertThat(saved.rating()).isEqualTo(5);
              assertThat(saved.createdAt()).isEqualTo(now);
              assertThat(saved.updatedAt()).isEqualTo(now);
            })
        .verifyComplete();

    ArgumentCaptor<FeedbackEntity> entityCaptor = ArgumentCaptor.forClass(FeedbackEntity.class);
    verify(r2dbcRepository).save(entityCaptor.capture());
    FeedbackEntity sentEntity = entityCaptor.getValue();
    assertThat(sentEntity.getId()).isEqualTo(feedbackId);
    assertThat(sentEntity.getPizzeriaId()).isEqualTo(DEFAULT_PIZZERIA_ID);
    assertThat(sentEntity.getUserId()).isEqualTo(userId);
    assertThat(sentEntity.getKind()).isEqualTo("SERVICE");
    assertThat(sentEntity.getStatus()).isEqualTo("OPEN");
    assertThat(sentEntity.getMessage()).isEqualTo("Great experience");
    assertThat(sentEntity.getRating()).isEqualTo(5);
    assertThat(sentEntity.getCreatedAt()).isEqualTo(now);
    assertThat(sentEntity.getUpdatedAt()).isEqualTo(now);
  }
}
