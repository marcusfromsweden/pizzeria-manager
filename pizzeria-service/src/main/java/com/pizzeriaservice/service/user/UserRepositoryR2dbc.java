package com.pizzeriaservice.service.user;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepositoryR2dbc extends ReactiveCrudRepository<UserEntity, UUID> {
  Mono<UserEntity> findByIdAndPizzeriaId(UUID id, UUID pizzeriaId);

  Mono<UserEntity> findByEmailAndPizzeriaId(String email, UUID pizzeriaId);

  Flux<UserEntity> findAllByPizzeriaId(UUID pizzeriaId);
}
