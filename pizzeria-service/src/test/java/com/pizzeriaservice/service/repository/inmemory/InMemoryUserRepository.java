package com.pizzeriaservice.service.repository.inmemory;

import com.pizzeriaservice.service.domain.model.User;
import com.pizzeriaservice.service.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class InMemoryUserRepository implements UserRepository {

  private final Map<UUID, User> storage = new ConcurrentHashMap<>();

  @Override
  public Mono<User> save(User user) {
    if (user == null || user.id() == null) {
      return Mono.error(new IllegalArgumentException("User and user.id must be non-null"));
    }
    storage.put(user.id(), user);
    return Mono.just(user);
  }

  @Override
  public Mono<User> findByIdAndPizzeriaId(UUID id, UUID pizzeriaId) {
    User user = storage.get(id);
    if (user != null && pizzeriaId != null && pizzeriaId.equals(user.pizzeriaId())) {
      return Mono.just(user);
    }
    return Mono.empty();
  }

  @Override
  public Mono<User> findByEmailAndPizzeriaId(String email, UUID pizzeriaId) {
    if (email == null || pizzeriaId == null) {
      return Mono.empty();
    }
    return Flux.fromIterable(storage.values())
        .filter(
            user -> email.equalsIgnoreCase(user.email()) && pizzeriaId.equals(user.pizzeriaId()))
        .next();
  }

  @Override
  public Flux<User> findAllByPizzeriaId(UUID pizzeriaId) {
    if (pizzeriaId == null) {
      return Flux.empty();
    }
    return Flux.fromIterable(storage.values()).filter(user -> pizzeriaId.equals(user.pizzeriaId()));
  }

  @Override
  public Mono<Void> deleteById(UUID id) {
    storage.remove(id);
    return Mono.empty();
  }

  public void clear() {
    storage.clear();
  }
}
