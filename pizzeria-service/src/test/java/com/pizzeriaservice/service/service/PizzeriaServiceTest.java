package com.pizzeriaservice.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzeriaservice.service.pizzeria.PizzeriaEntity;
import com.pizzeriaservice.service.pizzeria.PizzeriaRepository;
import com.pizzeriaservice.service.support.ResourceNotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PizzeriaServiceTest {

  private static final UUID DEFAULT_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID OTHER_PIZZERIA_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Mock private PizzeriaRepository pizzeriaRepository;

  private PizzeriaService pizzeriaService;
  private PizzeriaConfigParser configParser;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    configParser = new PizzeriaConfigParser(new ObjectMapper());
    pizzeriaService = new PizzeriaService(pizzeriaRepository, configParser);
  }

  // resolvePizzeriaId tests

  @Test
  void resolvePizzeriaIdReturnsIdForActiveCode() {
    PizzeriaEntity pizzeria = createPizzeria(DEFAULT_PIZZERIA_ID, "marios", true);

    when(pizzeriaRepository.findByCodeAndActiveTrue("marios")).thenReturn(Mono.just(pizzeria));

    StepVerifier.create(pizzeriaService.resolvePizzeriaId("marios"))
        .expectNext(DEFAULT_PIZZERIA_ID)
        .verifyComplete();
  }

  @Test
  void resolvePizzeriaIdThrowsForUnknownCode() {
    when(pizzeriaRepository.findByCodeAndActiveTrue("unknown")).thenReturn(Mono.empty());

    StepVerifier.create(pizzeriaService.resolvePizzeriaId("unknown"))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  @Test
  void resolvePizzeriaIdThrowsForInactiveCode() {
    // findByCodeAndActiveTrue returns empty for inactive pizzerias
    when(pizzeriaRepository.findByCodeAndActiveTrue("inactive")).thenReturn(Mono.empty());

    StepVerifier.create(pizzeriaService.resolvePizzeriaId("inactive"))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  @Test
  void resolvePizzeriaIdIsolatesDifferentPizzerias() {
    PizzeriaEntity pizzeriaOne = createPizzeria(DEFAULT_PIZZERIA_ID, "marios", true);
    PizzeriaEntity pizzeriaTwo = createPizzeria(OTHER_PIZZERIA_ID, "luigis", true);

    when(pizzeriaRepository.findByCodeAndActiveTrue("marios")).thenReturn(Mono.just(pizzeriaOne));
    when(pizzeriaRepository.findByCodeAndActiveTrue("luigis")).thenReturn(Mono.just(pizzeriaTwo));

    // Each code resolves to its own pizzeria
    StepVerifier.create(pizzeriaService.resolvePizzeriaId("marios"))
        .expectNext(DEFAULT_PIZZERIA_ID)
        .verifyComplete();

    StepVerifier.create(pizzeriaService.resolvePizzeriaId("luigis"))
        .expectNext(OTHER_PIZZERIA_ID)
        .verifyComplete();
  }

  // findById tests

  @Test
  void findByIdReturnsPizzeriaEntity() {
    PizzeriaEntity pizzeria = createPizzeria(DEFAULT_PIZZERIA_ID, "marios", true);

    when(pizzeriaRepository.findById(DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(pizzeria));

    StepVerifier.create(pizzeriaService.findById(DEFAULT_PIZZERIA_ID))
        .assertNext(
            result -> {
              assertThat(result.id()).isEqualTo(DEFAULT_PIZZERIA_ID);
              assertThat(result.code()).isEqualTo("marios");
              assertThat(result.active()).isTrue();
            })
        .verifyComplete();
  }

  @Test
  void findByIdThrowsForUnknownId() {
    UUID unknownId = UUID.randomUUID();
    when(pizzeriaRepository.findById(unknownId)).thenReturn(Mono.empty());

    StepVerifier.create(pizzeriaService.findById(unknownId))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  @Test
  void findByIdReturnsInactivePizzeria() {
    PizzeriaEntity inactive = createPizzeria(DEFAULT_PIZZERIA_ID, "closed", false);

    when(pizzeriaRepository.findById(DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(inactive));

    // findById returns pizzeria regardless of active status
    StepVerifier.create(pizzeriaService.findById(DEFAULT_PIZZERIA_ID))
        .assertNext(
            result -> {
              assertThat(result.active()).isFalse();
            })
        .verifyComplete();
  }

  // validatePizzeriaActive tests

  @Test
  void validatePizzeriaActiveCompletesForActivePizzeria() {
    PizzeriaEntity active = createPizzeria(DEFAULT_PIZZERIA_ID, "marios", true);

    when(pizzeriaRepository.findById(DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(active));

    StepVerifier.create(pizzeriaService.validatePizzeriaActive(DEFAULT_PIZZERIA_ID))
        .verifyComplete();
  }

  @Test
  void validatePizzeriaActiveThrowsForInactivePizzeria() {
    PizzeriaEntity inactive = createPizzeria(DEFAULT_PIZZERIA_ID, "closed", false);

    when(pizzeriaRepository.findById(DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(inactive));

    StepVerifier.create(pizzeriaService.validatePizzeriaActive(DEFAULT_PIZZERIA_ID))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  @Test
  void validatePizzeriaActiveThrowsForUnknownPizzeria() {
    UUID unknownId = UUID.randomUUID();
    when(pizzeriaRepository.findById(unknownId)).thenReturn(Mono.empty());

    StepVerifier.create(pizzeriaService.validatePizzeriaActive(unknownId))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  @Test
  void validatePizzeriaActiveThrowsForNullActiveField() {
    // Edge case: active field is null
    PizzeriaEntity nullActive = createPizzeria(DEFAULT_PIZZERIA_ID, "unknown", null);

    when(pizzeriaRepository.findById(DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(nullActive));

    StepVerifier.create(pizzeriaService.validatePizzeriaActive(DEFAULT_PIZZERIA_ID))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  // Cross-tenant isolation tests

  @Test
  void differentPizzeriasAreIndependent() {
    PizzeriaEntity pizzeriaA = createPizzeria(DEFAULT_PIZZERIA_ID, "marios", true);
    PizzeriaEntity pizzeriaB = createPizzeria(OTHER_PIZZERIA_ID, "luigis", true);

    when(pizzeriaRepository.findById(DEFAULT_PIZZERIA_ID)).thenReturn(Mono.just(pizzeriaA));
    when(pizzeriaRepository.findById(OTHER_PIZZERIA_ID)).thenReturn(Mono.just(pizzeriaB));

    // Each pizzeria returns its own data
    StepVerifier.create(pizzeriaService.findById(DEFAULT_PIZZERIA_ID))
        .assertNext(result -> assertThat(result.code()).isEqualTo("marios"))
        .verifyComplete();

    StepVerifier.create(pizzeriaService.findById(OTHER_PIZZERIA_ID))
        .assertNext(result -> assertThat(result.code()).isEqualTo("luigis"))
        .verifyComplete();
  }

  @Test
  void resolvePizzeriaIdCannotAccessOtherPizzeriaByCode() {
    // Setup: only "marios" exists
    PizzeriaEntity pizzeria = createPizzeria(DEFAULT_PIZZERIA_ID, "marios", true);
    when(pizzeriaRepository.findByCodeAndActiveTrue("marios")).thenReturn(Mono.just(pizzeria));
    when(pizzeriaRepository.findByCodeAndActiveTrue("luigis")).thenReturn(Mono.empty());

    // Can access marios
    StepVerifier.create(pizzeriaService.resolvePizzeriaId("marios"))
        .expectNext(DEFAULT_PIZZERIA_ID)
        .verifyComplete();

    // Cannot access luigis (doesn't exist)
    StepVerifier.create(pizzeriaService.resolvePizzeriaId("luigis"))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  // getInfoByCode tests

  @Test
  void getInfoByCodeReturnsBasicInfoForActivePizzeria() {
    PizzeriaEntity pizzeria = createPizzeria(DEFAULT_PIZZERIA_ID, "marios", true);

    when(pizzeriaRepository.findByCodeAndActiveTrue("marios")).thenReturn(Mono.just(pizzeria));

    StepVerifier.create(pizzeriaService.getInfoByCode("marios"))
        .assertNext(
            result -> {
              assertThat(result.code()).isEqualTo("marios");
              assertThat(result.name()).isEqualTo("marios Pizzeria");
              assertThat(result.currency()).isEqualTo("USD");
              assertThat(result.timezone()).isEqualTo("America/New_York");
            })
        .verifyComplete();
  }

  @Test
  void getInfoByCodeThrowsForUnknownCode() {
    when(pizzeriaRepository.findByCodeAndActiveTrue("unknown")).thenReturn(Mono.empty());

    StepVerifier.create(pizzeriaService.getInfoByCode("unknown"))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }

  @Test
  void getInfoByCodeReturnsOpeningHoursAndPhoneNumbers() {
    String config =
        """
        {
          "openingHours": {
            "monday": {"open": "10:00", "close": "22:00"},
            "friday": {"open": "10:00", "close": "23:00"}
          },
          "phoneNumbers": [
            {"label": "Orders", "number": "+46 40 123 456"}
          ]
        }
        """;
    PizzeriaEntity pizzeria = createPizzeriaWithConfig(DEFAULT_PIZZERIA_ID, "ramona", true, config);

    when(pizzeriaRepository.findByCodeAndActiveTrue("ramona")).thenReturn(Mono.just(pizzeria));

    StepVerifier.create(pizzeriaService.getInfoByCode("ramona"))
        .assertNext(
            result -> {
              assertThat(result.openingHours()).isNotNull();
              // monday() and friday() are now lists of time slots
              assertThat(result.openingHours().monday()).hasSize(1);
              assertThat(result.openingHours().monday().get(0).open()).isEqualTo("10:00");
              assertThat(result.openingHours().monday().get(0).close()).isEqualTo("22:00");
              assertThat(result.openingHours().friday()).hasSize(1);
              assertThat(result.openingHours().friday().get(0).open()).isEqualTo("10:00");
              assertThat(result.openingHours().friday().get(0).close()).isEqualTo("23:00");
              // tuesday has no hours configured, should be empty list
              assertThat(result.openingHours().tuesday()).isEmpty();

              assertThat(result.phoneNumbers()).hasSize(1);
              assertThat(result.phoneNumbers().get(0).label()).isEqualTo("Orders");
              assertThat(result.phoneNumbers().get(0).number()).isEqualTo("+46 40 123 456");
            })
        .verifyComplete();
  }

  @Test
  void getInfoByCodeReturnsNullOpeningHoursForEmptyConfig() {
    PizzeriaEntity pizzeria = createPizzeria(DEFAULT_PIZZERIA_ID, "marios", true);

    when(pizzeriaRepository.findByCodeAndActiveTrue("marios")).thenReturn(Mono.just(pizzeria));

    StepVerifier.create(pizzeriaService.getInfoByCode("marios"))
        .assertNext(
            result -> {
              assertThat(result.openingHours()).isNull();
              assertThat(result.phoneNumbers()).isEmpty();
            })
        .verifyComplete();
  }

  private PizzeriaEntity createPizzeria(UUID id, String code, Boolean active) {
    return createPizzeriaWithConfig(id, code, active, "{}");
  }

  private PizzeriaEntity createPizzeriaWithConfig(
      UUID id, String code, Boolean active, String config) {
    return new PizzeriaEntity(
        id,
        code,
        code + " Pizzeria",
        "USD",
        "America/New_York",
        config,
        "{}",
        active,
        Instant.parse("2024-01-01T00:00:00Z"),
        Instant.parse("2024-01-01T00:00:00Z"));
  }
}
