package com.pizzeriaservice.service.service;

import com.pizzeriaservice.api.dto.PizzeriaInfoResponse;
import com.pizzeriaservice.service.pizzeria.PizzeriaEntity;
import com.pizzeriaservice.service.pizzeria.PizzeriaRepository;
import com.pizzeriaservice.service.support.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PizzeriaService {

  private final PizzeriaRepository pizzeriaRepository;
  private final PizzeriaConfigParser configParser;

  /**
   * Resolves an active pizzeria by its code (used in URL paths).
   *
   * @param code The unique pizzeria code (e.g., "marios", "luigi")
   * @return The pizzeria ID if found and active
   * @throws ResourceNotFoundException if pizzeria not found or inactive
   */
  public Mono<UUID> resolvePizzeriaId(String code) {
    return pizzeriaRepository
        .findByCodeAndActiveTrue(code)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Pizzeria", code)))
        .map(PizzeriaEntity::id);
  }

  /**
   * Finds a pizzeria by its ID.
   *
   * @param id The pizzeria UUID
   * @return The pizzeria entity if found
   */
  public Mono<PizzeriaEntity> findById(UUID id) {
    return pizzeriaRepository
        .findById(id)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Pizzeria", id)));
  }

  /**
   * Validates that a pizzeria exists and is active.
   *
   * @param pizzeriaId The pizzeria UUID to validate
   * @return Mono completing if valid, error if not found or inactive
   */
  public Mono<Void> validatePizzeriaActive(UUID pizzeriaId) {
    return pizzeriaRepository
        .findById(pizzeriaId)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Pizzeria", pizzeriaId)))
        .flatMap(
            pizzeria -> {
              if (!Boolean.TRUE.equals(pizzeria.active())) {
                return Mono.error(new ResourceNotFoundException("Pizzeria", pizzeriaId));
              }
              return Mono.empty();
            });
  }

  /**
   * Gets pizzeria info by its code.
   *
   * @param code The unique pizzeria code (e.g., "ramonamalmo")
   * @return The pizzeria info response if found and active
   * @throws ResourceNotFoundException if pizzeria not found or inactive
   */
  public Mono<PizzeriaInfoResponse> getInfoByCode(String code) {
    return pizzeriaRepository
        .findByCodeAndActiveTrue(code)
        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Pizzeria", code)))
        .map(
            entity ->
                new PizzeriaInfoResponse(
                    entity.code(),
                    entity.name(),
                    entity.currency(),
                    entity.timezone(),
                    configParser.parseAddress(entity.config()),
                    configParser.parseOpeningHours(entity.config()),
                    configParser.parsePhoneNumbers(entity.config())));
  }
}
