package com.pizzeriaservice.service.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.pizzeriaservice.service.test.PizzeriaIntegrationTest;
import com.pizzeriaservice.service.test.PostgresContainerSupport;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;

@PizzeriaIntegrationTest
class MultiTenantSchemaIntegrationTest extends PostgresContainerSupport {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID RAMONA_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Autowired private DatabaseClient databaseClient;

  @Test
  void shouldCreatePizzeriasTable() {
    StepVerifier.create(
            databaseClient
                .sql(
                    "SELECT column_name FROM information_schema.columns WHERE table_name = 'pizzerias' ORDER BY ordinal_position")
                .map(row -> row.get("column_name", String.class))
                .all()
                .collectList())
        .assertNext(
            columns -> {
              assertThat(columns)
                  .containsExactly(
                      "id",
                      "code",
                      "name",
                      "currency",
                      "timezone",
                      "config",
                      "branding",
                      "active",
                      "created_at",
                      "updated_at");
            })
        .verifyComplete();
  }

  @Test
  void shouldInsertDefaultPizzeria() {
    StepVerifier.create(
            databaseClient
                .sql(
                    "SELECT id, code, name, currency, timezone, active FROM pizzerias WHERE id = :id")
                .bind("id", DEFAULT_PIZZERIA_ID)
                .map(
                    row ->
                        Map.of(
                            "id", row.get("id", UUID.class),
                            "code", row.get("code", String.class),
                            "name", row.get("name", String.class),
                            "currency", row.get("currency", String.class),
                            "timezone", row.get("timezone", String.class),
                            "active", row.get("active", Boolean.class)))
                .one())
        .assertNext(
            pizzeria -> {
              // Default pizzeria is deactivated after migration to Ramona
              assertThat(pizzeria.get("id")).isEqualTo(DEFAULT_PIZZERIA_ID);
              assertThat(pizzeria.get("code")).isEqualTo("default");
              assertThat(pizzeria.get("name")).isEqualTo("Default Pizzeria");
              assertThat(pizzeria.get("currency")).isEqualTo("SEK");
              assertThat(pizzeria.get("timezone")).isEqualTo("Europe/Stockholm");
              assertThat(pizzeria.get("active")).isEqualTo(false);
            })
        .verifyComplete();
  }

  @Test
  void shouldAddPizzeriaIdToMenuSections() {
    // Menu data migrated to Ramona pizzeria
    StepVerifier.create(
            databaseClient
                .sql("SELECT pizzeria_id FROM menu_sections LIMIT 1")
                .map(row -> row.get("pizzeria_id", UUID.class))
                .one())
        .assertNext(pizzeriaId -> assertThat(pizzeriaId).isEqualTo(RAMONA_PIZZERIA_ID))
        .verifyComplete();
  }

  @Test
  void shouldAddPizzeriaIdToMenuItems() {
    // Menu data migrated to Ramona pizzeria
    StepVerifier.create(
            databaseClient
                .sql("SELECT pizzeria_id FROM menu_items LIMIT 1")
                .map(row -> row.get("pizzeria_id", UUID.class))
                .one())
        .assertNext(pizzeriaId -> assertThat(pizzeriaId).isEqualTo(RAMONA_PIZZERIA_ID))
        .verifyComplete();
  }

  @Test
  void shouldAddPizzeriaIdToMenuIngredientFacts() {
    // Menu data migrated to Ramona pizzeria
    StepVerifier.create(
            databaseClient
                .sql("SELECT pizzeria_id FROM menu_ingredient_facts LIMIT 1")
                .map(row -> row.get("pizzeria_id", UUID.class))
                .one())
        .assertNext(pizzeriaId -> assertThat(pizzeriaId).isEqualTo(RAMONA_PIZZERIA_ID))
        .verifyComplete();
  }

  @Test
  void shouldAddPizzeriaIdToPizzaCustomisations() {
    // Menu data migrated to Ramona pizzeria
    StepVerifier.create(
            databaseClient
                .sql("SELECT pizzeria_id FROM pizza_customisations LIMIT 1")
                .map(row -> row.get("pizzeria_id", UUID.class))
                .one())
        .assertNext(pizzeriaId -> assertThat(pizzeriaId).isEqualTo(RAMONA_PIZZERIA_ID))
        .verifyComplete();
  }

  @Test
  void shouldHavePizzeriaIdColumnInUsersTable() {
    StepVerifier.create(
            databaseClient
                .sql(
                    "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_name = 'users' AND column_name = 'pizzeria_id'")
                .map(row -> row.get("column_name", String.class))
                .one())
        .assertNext(columnName -> assertThat(columnName).isEqualTo("pizzeria_id"))
        .verifyComplete();
  }

  @Test
  void shouldHavePizzeriaIdColumnInFeedbackTable() {
    StepVerifier.create(
            databaseClient
                .sql(
                    "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_name = 'feedback' AND column_name = 'pizzeria_id'")
                .map(row -> row.get("column_name", String.class))
                .one())
        .assertNext(columnName -> assertThat(columnName).isEqualTo("pizzeria_id"))
        .verifyComplete();
  }

  @Test
  void shouldHavePizzeriaIdColumnInPizzaScoresTable() {
    StepVerifier.create(
            databaseClient
                .sql(
                    "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_name = 'pizza_scores' AND column_name = 'pizzeria_id'")
                .map(row -> row.get("column_name", String.class))
                .one())
        .assertNext(columnName -> assertThat(columnName).isEqualTo("pizzeria_id"))
        .verifyComplete();
  }

  @Test
  void shouldHavePizzeriaIdColumnInUserPreferredIngredientsTable() {
    StepVerifier.create(
            databaseClient
                .sql(
                    "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_name = 'user_preferred_ingredients' AND column_name = 'pizzeria_id'")
                .map(row -> row.get("column_name", String.class))
                .one())
        .assertNext(columnName -> assertThat(columnName).isEqualTo("pizzeria_id"))
        .verifyComplete();
  }

  @Test
  void shouldHaveForeignKeyFromMenuSectionsToPizzerias() {
    StepVerifier.create(
            databaseClient
                .sql(
                    "SELECT constraint_name FROM information_schema.table_constraints "
                        + "WHERE table_name = 'menu_sections' AND constraint_type = 'FOREIGN KEY' "
                        + "AND constraint_name = 'fk_menu_sections_pizzeria'")
                .map(row -> row.get("constraint_name", String.class))
                .one())
        .assertNext(
            constraintName -> assertThat(constraintName).isEqualTo("fk_menu_sections_pizzeria"))
        .verifyComplete();
  }

  @Test
  void shouldHaveForeignKeyFromMenuItemsToPizzerias() {
    StepVerifier.create(
            databaseClient
                .sql(
                    "SELECT constraint_name FROM information_schema.table_constraints "
                        + "WHERE table_name = 'menu_items' AND constraint_type = 'FOREIGN KEY' "
                        + "AND constraint_name = 'fk_menu_items_pizzeria'")
                .map(row -> row.get("constraint_name", String.class))
                .one())
        .assertNext(
            constraintName -> assertThat(constraintName).isEqualTo("fk_menu_items_pizzeria"))
        .verifyComplete();
  }

  @Test
  void shouldHaveForeignKeyFromUsersToPizzerias() {
    StepVerifier.create(
            databaseClient
                .sql(
                    "SELECT constraint_name FROM information_schema.table_constraints "
                        + "WHERE table_name = 'users' AND constraint_type = 'FOREIGN KEY' "
                        + "AND constraint_name = 'fk_users_pizzeria'")
                .map(row -> row.get("constraint_name", String.class))
                .one())
        .assertNext(constraintName -> assertThat(constraintName).isEqualTo("fk_users_pizzeria"))
        .verifyComplete();
  }

  @Test
  void shouldAllowSameEmailInDifferentPizzerias() {
    UUID secondPizzeriaId = UUID.randomUUID();
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();
    String sharedEmail = "shared@example.com";

    var pipeline =
        databaseClient
            .sql(
                "INSERT INTO pizzerias (id, code, name, currency, timezone, active) "
                    + "VALUES (:id, :code, :name, :currency, :timezone, :active)")
            .bind("id", secondPizzeriaId)
            .bind("code", "second-pizzeria")
            .bind("name", "Second Pizzeria")
            .bind("currency", "EUR")
            .bind("timezone", "Europe/Rome")
            .bind("active", true)
            .fetch()
            .rowsUpdated()
            .then(
                databaseClient
                    .sql(
                        "INSERT INTO users (id, pizzeria_id, name, email, password_hash, email_verified, preferred_diet, status) "
                            + "VALUES (:id, :pizzeria_id, :name, :email, :password_hash, :email_verified, :preferred_diet, :status)")
                    .bind("id", userId1)
                    .bind("pizzeria_id", DEFAULT_PIZZERIA_ID)
                    .bind("name", "User One")
                    .bind("email", sharedEmail)
                    .bind("password_hash", "hash1")
                    .bind("email_verified", true)
                    .bind("preferred_diet", "NONE")
                    .bind("status", "ACTIVE")
                    .fetch()
                    .rowsUpdated())
            .then(
                databaseClient
                    .sql(
                        "INSERT INTO users (id, pizzeria_id, name, email, password_hash, email_verified, preferred_diet, status) "
                            + "VALUES (:id, :pizzeria_id, :name, :email, :password_hash, :email_verified, :preferred_diet, :status)")
                    .bind("id", userId2)
                    .bind("pizzeria_id", secondPizzeriaId)
                    .bind("name", "User Two")
                    .bind("email", sharedEmail)
                    .bind("password_hash", "hash2")
                    .bind("email_verified", true)
                    .bind("preferred_diet", "VEGAN")
                    .bind("status", "ACTIVE")
                    .fetch()
                    .rowsUpdated())
            .then(
                databaseClient
                    .sql("SELECT COUNT(*) as count FROM users WHERE email = :email")
                    .bind("email", sharedEmail)
                    .map(row -> row.get("count", Long.class))
                    .one());

    StepVerifier.create(pipeline)
        .assertNext(count -> assertThat(count).isEqualTo(2L))
        .verifyComplete();
  }

  @Test
  void shouldRejectDuplicateEmailWithinSamePizzeria() {
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();
    String duplicateEmail = "duplicate-" + UUID.randomUUID() + "@example.com";

    var pipeline =
        databaseClient
            .sql(
                "INSERT INTO users (id, pizzeria_id, name, email, password_hash, email_verified, preferred_diet, status) "
                    + "VALUES (:id, :pizzeria_id, :name, :email, :password_hash, :email_verified, :preferred_diet, :status)")
            .bind("id", userId1)
            .bind("pizzeria_id", DEFAULT_PIZZERIA_ID)
            .bind("name", "First User")
            .bind("email", duplicateEmail)
            .bind("password_hash", "hash1")
            .bind("email_verified", true)
            .bind("preferred_diet", "NONE")
            .bind("status", "ACTIVE")
            .fetch()
            .rowsUpdated()
            .then(
                databaseClient
                    .sql(
                        "INSERT INTO users (id, pizzeria_id, name, email, password_hash, email_verified, preferred_diet, status) "
                            + "VALUES (:id, :pizzeria_id, :name, :email, :password_hash, :email_verified, :preferred_diet, :status)")
                    .bind("id", userId2)
                    .bind("pizzeria_id", DEFAULT_PIZZERIA_ID)
                    .bind("name", "Second User")
                    .bind("email", duplicateEmail)
                    .bind("password_hash", "hash2")
                    .bind("email_verified", true)
                    .bind("preferred_diet", "VEGAN")
                    .bind("status", "ACTIVE")
                    .fetch()
                    .rowsUpdated());

    StepVerifier.create(pipeline).expectError().verify();
  }

  @Test
  void shouldHaveIndexesOnPizzeriaIdColumns() {
    List<String> expectedIndexes =
        List.of(
            "idx_delivery_addresses_user_pizzeria",
            "idx_feedback_pizzeria",
            "idx_menu_ingredient_facts_pizzeria",
            "idx_menu_items_pizzeria",
            "idx_menu_sections_pizzeria",
            "idx_orders_user_pizzeria",
            "idx_pizza_customisations_pizzeria",
            "idx_pizza_scores_pizzeria",
            "idx_user_preferred_ingredients_pizzeria",
            "idx_users_pizzeria");

    StepVerifier.create(
            databaseClient
                .sql(
                    "SELECT indexname FROM pg_indexes WHERE indexname LIKE 'idx_%_pizzeria' ORDER BY indexname")
                .map(row -> row.get("indexname", String.class))
                .all()
                .collectList())
        .assertNext(
            indexes -> {
              assertThat(indexes).containsExactlyInAnyOrderElementsOf(expectedIndexes);
            })
        .verifyComplete();
  }

  @Test
  void shouldCascadeDeleteWhenPizzeriaIsDeleted() {
    UUID pizzeriaId = UUID.randomUUID();
    UUID sectionId = UUID.randomUUID();

    var pipeline =
        databaseClient
            .sql(
                "INSERT INTO pizzerias (id, code, name, currency, timezone, active) "
                    + "VALUES (:id, :code, :name, :currency, :timezone, :active)")
            .bind("id", pizzeriaId)
            .bind("code", "cascade-test-" + pizzeriaId)
            .bind("name", "Cascade Test Pizzeria")
            .bind("currency", "SEK")
            .bind("timezone", "Europe/Stockholm")
            .bind("active", true)
            .fetch()
            .rowsUpdated()
            .then(
                databaseClient
                    .sql(
                        "INSERT INTO menu_sections (id, pizzeria_id, code, translation_key, sort_order) "
                            + "VALUES (:id, :pizzeria_id, :code, :translation_key, :sort_order)")
                    .bind("id", sectionId)
                    .bind("pizzeria_id", pizzeriaId)
                    .bind("code", "test-section")
                    .bind("translation_key", "test.section")
                    .bind("sort_order", 1)
                    .fetch()
                    .rowsUpdated())
            .then(
                databaseClient
                    .sql("DELETE FROM pizzerias WHERE id = :id")
                    .bind("id", pizzeriaId)
                    .fetch()
                    .rowsUpdated())
            .then(
                databaseClient
                    .sql("SELECT COUNT(*) as count FROM menu_sections WHERE id = :id")
                    .bind("id", sectionId)
                    .map(row -> row.get("count", Long.class))
                    .one());

    StepVerifier.create(pipeline)
        .assertNext(count -> assertThat(count).isEqualTo(0L))
        .verifyComplete();
  }

  @Test
  void shouldEnforceUniquePizzeriaCode() {
    UUID pizzeriaId = UUID.randomUUID();
    String duplicateCode = "duplicate-code-" + UUID.randomUUID();

    var pipeline =
        databaseClient
            .sql(
                "INSERT INTO pizzerias (id, code, name, currency, timezone, active) "
                    + "VALUES (:id, :code, :name, :currency, :timezone, :active)")
            .bind("id", pizzeriaId)
            .bind("code", duplicateCode)
            .bind("name", "First Pizzeria")
            .bind("currency", "SEK")
            .bind("timezone", "Europe/Stockholm")
            .bind("active", true)
            .fetch()
            .rowsUpdated()
            .then(
                databaseClient
                    .sql(
                        "INSERT INTO pizzerias (id, code, name, currency, timezone, active) "
                            + "VALUES (:id, :code, :name, :currency, :timezone, :active)")
                    .bind("id", UUID.randomUUID())
                    .bind("code", duplicateCode)
                    .bind("name", "Second Pizzeria")
                    .bind("currency", "EUR")
                    .bind("timezone", "Europe/Rome")
                    .bind("active", true)
                    .fetch()
                    .rowsUpdated());

    StepVerifier.create(pipeline).expectError().verify();
  }
}
