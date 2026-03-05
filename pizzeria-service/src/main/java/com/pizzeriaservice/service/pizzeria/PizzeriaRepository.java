package com.pizzeriaservice.service.pizzeria;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PizzeriaRepository extends ReactiveCrudRepository<PizzeriaEntity, UUID> {

  Mono<PizzeriaEntity> findByCode(String code);

  Mono<PizzeriaEntity> findByCodeAndActiveTrue(String code);
}
