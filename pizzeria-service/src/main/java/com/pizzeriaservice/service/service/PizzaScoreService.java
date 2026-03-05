package com.pizzeriaservice.service.service;

import com.pizzeriaservice.api.dto.PizzaScoreCreateRequest;
import com.pizzeriaservice.api.dto.PizzaScoreResponse;
import com.pizzeriaservice.service.converter.RestDomainConverter;
import com.pizzeriaservice.service.domain.model.PizzaScore;
import com.pizzeriaservice.service.repository.PizzaScoreRepository;
import com.pizzeriaservice.service.support.TimeProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class PizzaScoreService {

  private final PizzaScoreRepository repository;
  private final TimeProvider timeProvider;
  private final RestDomainConverter converter;

  public Mono<PizzaScoreResponse> create(
      UUID userId, UUID pizzeriaId, PizzaScoreCreateRequest request) {
    PizzaScore score =
        PizzaScore.builder()
            .id(UUID.randomUUID())
            .pizzeriaId(pizzeriaId)
            .userId(userId)
            .pizzaId(request.pizzaId())
            .pizzaKind(RestDomainConverter.fromPizzaType(request.pizzaType()))
            .score(request.score())
            .comment(request.comment())
            .createdAt(timeProvider.now())
            .createdBy(userId)
            .isNew(true)
            .build();

    return repository.save(score).map(converter::toPizzaScoreResponse);
  }

  public Flux<PizzaScoreResponse> getForUser(UUID userId, UUID pizzeriaId) {
    return repository
        .findByUserIdAndPizzeriaId(userId, pizzeriaId)
        .map(converter::toPizzaScoreResponse);
  }
}
