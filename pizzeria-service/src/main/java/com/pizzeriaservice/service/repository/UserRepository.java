package com.pizzeriaservice.service.repository;

import com.pizzeriaservice.service.domain.model.User;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository {
  Mono<User> save(User user);

  Mono<User> findByIdAndPizzeriaId(UUID id, UUID pizzeriaId);

  Mono<User> findByEmailAndPizzeriaId(String email, UUID pizzeriaId);

  Flux<User> findAllByPizzeriaId(UUID pizzeriaId);

  Mono<Void> deleteById(UUID id);
}
