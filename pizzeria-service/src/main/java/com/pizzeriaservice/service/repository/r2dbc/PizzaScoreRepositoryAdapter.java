package com.pizzeriaservice.service.repository.r2dbc;

import com.pizzeriaservice.service.domain.PizzaKind;
import com.pizzeriaservice.service.domain.model.PizzaScore;
import com.pizzeriaservice.service.pizzascore.PizzaScoreEntity;
import com.pizzeriaservice.service.pizzascore.PizzaScoreRepositoryR2dbc;
import com.pizzeriaservice.service.repository.PizzaScoreRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class PizzaScoreRepositoryAdapter implements PizzaScoreRepository {

  private final PizzaScoreRepositoryR2dbc repository;

  @Override
  public Mono<PizzaScore> save(PizzaScore score) {
    PizzaScoreEntity entity = toEntity(score);
    return repository.save(entity).map(PizzaScoreRepositoryAdapter::toDomain);
  }

  @Override
  public Flux<PizzaScore> findByUserId(UUID userId) {
    return repository.findAllByUserId(userId).map(PizzaScoreRepositoryAdapter::toDomain);
  }

  @Override
  public Flux<PizzaScore> findByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId) {
    return repository
        .findAllByUserIdAndPizzeriaId(userId, pizzeriaId)
        .map(PizzaScoreRepositoryAdapter::toDomain);
  }

  private static PizzaScoreEntity toEntity(PizzaScore score) {
    if (score.isNew()) {
      return PizzaScoreEntity.builder()
          .id(score.id())
          .pizzeriaId(score.pizzeriaId())
          .userId(score.userId())
          .pizzaId(score.pizzaId())
          .pizzaKind(score.pizzaKind().name())
          .score(score.score())
          .comment(score.comment())
          .createdAt(score.createdAt())
          .createdBy(score.createdBy())
          .isNew(true)
          .build();
    }
    return PizzaScoreEntity.builder()
        .id(score.id())
        .pizzeriaId(score.pizzeriaId())
        .userId(score.userId())
        .pizzaId(score.pizzaId())
        .pizzaKind(score.pizzaKind().name())
        .score(score.score())
        .comment(score.comment())
        .createdAt(score.createdAt())
        .createdBy(score.createdBy())
        .build();
  }

  private static PizzaScore toDomain(PizzaScoreEntity entity) {
    return PizzaScore.builder()
        .id(entity.getId())
        .pizzeriaId(entity.getPizzeriaId())
        .userId(entity.getUserId())
        .pizzaId(entity.getPizzaId())
        .pizzaKind(PizzaKind.valueOf(entity.getPizzaKind()))
        .score(entity.getScore())
        .comment(entity.getComment())
        .createdAt(entity.getCreatedAt())
        .createdBy(entity.getCreatedBy())
        .isNew(false)
        .build();
  }
}
