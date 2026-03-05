package com.pizzeriaservice.service.repository.r2dbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.pizzeriaservice.service.domain.FeedbackKind;
import com.pizzeriaservice.service.domain.FeedbackStatus;
import com.pizzeriaservice.service.domain.model.Feedback;
import com.pizzeriaservice.service.feedback.FeedbackEntity;
import com.pizzeriaservice.service.feedback.FeedbackRepositoryR2dbc;
import com.pizzeriaservice.service.test.PizzeriaIntegrationTest;
import com.pizzeriaservice.service.test.PostgresContainerSupport;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@PizzeriaIntegrationTest
public class FeedbackRepositoryAdapterIntegrationTest extends PostgresContainerSupport {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Autowired private FeedbackRepositoryAdapter feedbackRepositoryAdapter;
  @Autowired private FeedbackRepositoryR2dbc feedbackRepositoryR2dbc;
  @Autowired private DatabaseClient databaseClient;

  private UUID testUserId;

  @BeforeEach
  void setUp() {
    testUserId = UUID.randomUUID();
    Instant now = Instant.now();

    databaseClient
        .sql(
            """
            INSERT INTO users (id, pizzeria_id, name, email, password_hash, email_verified, preferred_diet, status, created_at, updated_at)
            VALUES (:id, :pizzeria_id, :name, :email, :password_hash, :email_verified, :preferred_diet, :status, :created_at, :updated_at)
            ON CONFLICT (pizzeria_id, email) DO NOTHING
            """)
        .bind("id", testUserId)
        .bind("pizzeria_id", DEFAULT_PIZZERIA_ID)
        .bind("name", "Integration User")
        .bind("email", "integration-" + testUserId + "@example.com")
        .bind("password_hash", "hash")
        .bind("email_verified", true)
        .bind("preferred_diet", "NONE")
        .bind("status", "ACTIVE")
        .bind("created_at", now)
        .bind("updated_at", now)
        .fetch()
        .rowsUpdated()
        .block();
  }

  @Test
  void savePersistsFeedbackWithMappedFields() {
    UUID feedbackId = UUID.randomUUID();
    Instant now = Instant.parse("2024-05-05T12:30:00Z");

    Mono<Tuple2<Feedback, FeedbackEntity>> pipeline =
        databaseClient
            .sql(
                """
            INSERT INTO feedback (id, pizzeria_id, user_id, kind, message, rating, status, created_at, updated_at)
            VALUES (:id, :pizzeria_id, :user_id, :kind, :message, :rating, :status, :created_at, :updated_at)
            """)
            .bind("id", feedbackId)
            .bind("pizzeria_id", DEFAULT_PIZZERIA_ID)
            .bind("user_id", testUserId)
            .bind("kind", "SERVICE")
            .bind("message", "Old message")
            .bind("rating", 3)
            .bind("status", "OPEN")
            .bind("created_at", now.minusSeconds(30))
            .bind("updated_at", now.minusSeconds(30))
            .fetch()
            .rowsUpdated()
            .then(
                feedbackRepositoryAdapter
                    .save(
                        Feedback.builder()
                            .id(feedbackId)
                            .pizzeriaId(DEFAULT_PIZZERIA_ID)
                            .userId(testUserId)
                            .kind(FeedbackKind.SERVICE)
                            .message("Integration feedback")
                            .rating(4)
                            .status(FeedbackStatus.OPEN)
                            .createdAt(now.minusSeconds(30))
                            .updatedAt(now)
                            .build())
                    .flatMap(
                        saved ->
                            feedbackRepositoryR2dbc
                                .findById(saved.id())
                                .map(entity -> Tuples.of(saved, entity))));

    StepVerifier.create(pipeline)
        .assertNext(
            tuple -> {
              Feedback saved = tuple.getT1();
              FeedbackEntity entity = tuple.getT2();

              assertThat(saved.id()).isEqualTo(feedbackId);
              assertThat(saved.pizzeriaId()).isEqualTo(DEFAULT_PIZZERIA_ID);
              assertThat(saved.kind()).isEqualTo(FeedbackKind.SERVICE);
              assertThat(saved.status()).isEqualTo(FeedbackStatus.OPEN);
              assertThat(saved.message()).isEqualTo("Integration feedback");
              assertThat(saved.rating()).isEqualTo(4);

              assertThat(entity.getId()).isEqualTo(feedbackId);
              assertThat(entity.getPizzeriaId()).isEqualTo(DEFAULT_PIZZERIA_ID);
              assertThat(entity.getUserId()).isEqualTo(testUserId);
              assertThat(entity.getKind()).isEqualTo("SERVICE");
              assertThat(entity.getStatus()).isEqualTo("OPEN");
              assertThat(entity.getMessage()).isEqualTo("Integration feedback");
              assertThat(entity.getRating()).isEqualTo(4);
            })
        .verifyComplete();
  }
}
