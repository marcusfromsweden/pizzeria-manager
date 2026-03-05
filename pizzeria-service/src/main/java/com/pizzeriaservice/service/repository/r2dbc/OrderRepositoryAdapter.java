package com.pizzeriaservice.service.repository.r2dbc;

import com.pizzeriaservice.service.domain.FulfillmentType;
import com.pizzeriaservice.service.domain.OrderStatus;
import com.pizzeriaservice.service.domain.PizzaSize;
import com.pizzeriaservice.service.domain.model.Order;
import com.pizzeriaservice.service.domain.model.OrderItem;
import com.pizzeriaservice.service.domain.model.OrderItemCustomisation;
import com.pizzeriaservice.service.order.OrderEntity;
import com.pizzeriaservice.service.order.OrderItemCustomisationEntity;
import com.pizzeriaservice.service.order.OrderItemCustomisationRepositoryR2dbc;
import com.pizzeriaservice.service.order.OrderItemEntity;
import com.pizzeriaservice.service.order.OrderItemRepositoryR2dbc;
import com.pizzeriaservice.service.order.OrderRepositoryR2dbc;
import com.pizzeriaservice.service.repository.OrderRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

  private final OrderRepositoryR2dbc orderRepository;
  private final OrderItemRepositoryR2dbc orderItemRepository;
  private final OrderItemCustomisationRepositoryR2dbc orderItemCustomisationRepository;

  @Override
  public Mono<Order> save(Order order) {
    OrderEntity entity = toEntity(order);
    return orderRepository.save(entity).map(OrderRepositoryAdapter::toDomain);
  }

  @Override
  public Mono<OrderItem> saveOrderItem(OrderItem item) {
    OrderItemEntity entity = toEntity(item);
    return orderItemRepository.save(entity).map(OrderRepositoryAdapter::toDomain);
  }

  @Override
  public Mono<OrderItemCustomisation> saveOrderItemCustomisation(
      OrderItemCustomisation customisation) {
    OrderItemCustomisationEntity entity = toEntity(customisation);
    return orderItemCustomisationRepository.save(entity).map(OrderRepositoryAdapter::toDomain);
  }

  @Override
  public Flux<Order> findByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId) {
    return orderRepository
        .findAllByUserIdAndPizzeriaIdOrderByCreatedAtDesc(userId, pizzeriaId)
        .map(OrderRepositoryAdapter::toDomain);
  }

  @Override
  public Flux<Order> findActiveByUserIdAndPizzeriaId(UUID userId, UUID pizzeriaId) {
    return orderRepository
        .findActiveByUserIdAndPizzeriaId(userId, pizzeriaId)
        .map(OrderRepositoryAdapter::toDomain);
  }

  @Override
  public Mono<Order> findByIdAndUserIdAndPizzeriaId(UUID id, UUID userId, UUID pizzeriaId) {
    return orderRepository
        .findByIdAndUserIdAndPizzeriaId(id, userId, pizzeriaId)
        .map(OrderRepositoryAdapter::toDomain);
  }

  @Override
  public Mono<Order> findByIdAndPizzeriaId(UUID id, UUID pizzeriaId) {
    return orderRepository
        .findByIdAndPizzeriaId(id, pizzeriaId)
        .map(OrderRepositoryAdapter::toDomain);
  }

  @Override
  public Flux<OrderItem> findItemsByOrderId(UUID orderId) {
    return orderItemRepository
        .findAllByOrderIdOrderByCreatedAt(orderId)
        .map(OrderRepositoryAdapter::toDomain);
  }

  @Override
  public Flux<OrderItem> findItemsByOrderIds(Collection<UUID> orderIds) {
    return orderItemRepository.findAllByOrderIdIn(orderIds).map(OrderRepositoryAdapter::toDomain);
  }

  @Override
  public Flux<OrderItemCustomisation> findCustomisationsByOrderItemId(UUID orderItemId) {
    return orderItemCustomisationRepository
        .findAllByOrderItemIdOrderByCreatedAt(orderItemId)
        .map(OrderRepositoryAdapter::toDomain);
  }

  @Override
  public Flux<OrderItemCustomisation> findCustomisationsByOrderItemIds(
      Collection<UUID> orderItemIds) {
    return orderItemCustomisationRepository
        .findAllByOrderItemIdIn(orderItemIds)
        .map(OrderRepositoryAdapter::toDomain);
  }

  @Override
  public Mono<Integer> findMaxOrderNumberForPrefix(UUID pizzeriaId, String prefix) {
    return orderRepository.findMaxOrderNumberForPrefix(pizzeriaId, prefix);
  }

  private static OrderEntity toEntity(Order order) {
    if (order.isNew()) {
      return OrderEntity.builder()
          .id(order.id())
          .pizzeriaId(order.pizzeriaId())
          .userId(order.userId())
          .orderNumber(order.orderNumber())
          .status(order.status().name())
          .fulfillmentType(order.fulfillmentType().name())
          .deliveryAddressId(order.deliveryAddressId())
          .deliveryStreet(order.deliveryStreet())
          .deliveryPostalCode(order.deliveryPostalCode())
          .deliveryCity(order.deliveryCity())
          .deliveryPhone(order.deliveryPhone())
          .deliveryInstructions(order.deliveryInstructions())
          .requestedTime(order.requestedTime())
          .estimatedReadyTime(order.estimatedReadyTime())
          .subtotal(order.subtotal())
          .deliveryFee(order.deliveryFee())
          .total(order.total())
          .customerNotes(order.customerNotes())
          .createdAt(order.createdAt())
          .updatedAt(order.updatedAt())
          .createdBy(order.createdBy())
          .updatedBy(order.updatedBy())
          .isNew(true)
          .build();
    }
    return OrderEntity.builder()
        .id(order.id())
        .pizzeriaId(order.pizzeriaId())
        .userId(order.userId())
        .orderNumber(order.orderNumber())
        .status(order.status().name())
        .fulfillmentType(order.fulfillmentType().name())
        .deliveryAddressId(order.deliveryAddressId())
        .deliveryStreet(order.deliveryStreet())
        .deliveryPostalCode(order.deliveryPostalCode())
        .deliveryCity(order.deliveryCity())
        .deliveryPhone(order.deliveryPhone())
        .deliveryInstructions(order.deliveryInstructions())
        .requestedTime(order.requestedTime())
        .estimatedReadyTime(order.estimatedReadyTime())
        .subtotal(order.subtotal())
        .deliveryFee(order.deliveryFee())
        .total(order.total())
        .customerNotes(order.customerNotes())
        .createdAt(order.createdAt())
        .updatedAt(order.updatedAt())
        .createdBy(order.createdBy())
        .updatedBy(order.updatedBy())
        .build();
  }

  private static Order toDomain(OrderEntity entity) {
    return Order.builder()
        .id(entity.getId())
        .pizzeriaId(entity.getPizzeriaId())
        .userId(entity.getUserId())
        .orderNumber(entity.getOrderNumber())
        .status(OrderStatus.valueOf(entity.getStatus()))
        .fulfillmentType(FulfillmentType.valueOf(entity.getFulfillmentType()))
        .deliveryAddressId(entity.getDeliveryAddressId())
        .deliveryStreet(entity.getDeliveryStreet())
        .deliveryPostalCode(entity.getDeliveryPostalCode())
        .deliveryCity(entity.getDeliveryCity())
        .deliveryPhone(entity.getDeliveryPhone())
        .deliveryInstructions(entity.getDeliveryInstructions())
        .requestedTime(entity.getRequestedTime())
        .estimatedReadyTime(entity.getEstimatedReadyTime())
        .subtotal(entity.getSubtotal())
        .deliveryFee(entity.getDeliveryFee())
        .total(entity.getTotal())
        .customerNotes(entity.getCustomerNotes())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .createdBy(entity.getCreatedBy())
        .updatedBy(entity.getUpdatedBy())
        .items(Collections.emptyList())
        .isNew(false)
        .build();
  }

  private static OrderItemEntity toEntity(OrderItem item) {
    if (item.isNew()) {
      return OrderItemEntity.builder()
          .id(item.id())
          .pizzeriaId(item.pizzeriaId())
          .orderId(item.orderId())
          .menuItemId(item.menuItemId())
          .menuItemNameKey(item.menuItemNameKey())
          .size(item.size().name())
          .quantity(item.quantity())
          .basePrice(item.basePrice())
          .customisationsPrice(item.customisationsPrice())
          .itemTotal(item.itemTotal())
          .specialInstructions(item.specialInstructions())
          .createdAt(item.createdAt())
          .createdBy(item.createdBy())
          .isNew(true)
          .build();
    }
    return OrderItemEntity.builder()
        .id(item.id())
        .pizzeriaId(item.pizzeriaId())
        .orderId(item.orderId())
        .menuItemId(item.menuItemId())
        .menuItemNameKey(item.menuItemNameKey())
        .size(item.size().name())
        .quantity(item.quantity())
        .basePrice(item.basePrice())
        .customisationsPrice(item.customisationsPrice())
        .itemTotal(item.itemTotal())
        .specialInstructions(item.specialInstructions())
        .createdAt(item.createdAt())
        .createdBy(item.createdBy())
        .build();
  }

  private static OrderItem toDomain(OrderItemEntity entity) {
    return OrderItem.builder()
        .id(entity.getId())
        .pizzeriaId(entity.getPizzeriaId())
        .orderId(entity.getOrderId())
        .menuItemId(entity.getMenuItemId())
        .menuItemNameKey(entity.getMenuItemNameKey())
        .size(PizzaSize.valueOf(entity.getSize()))
        .quantity(entity.getQuantity())
        .basePrice(entity.getBasePrice())
        .customisationsPrice(entity.getCustomisationsPrice())
        .itemTotal(entity.getItemTotal())
        .specialInstructions(entity.getSpecialInstructions())
        .createdAt(entity.getCreatedAt())
        .createdBy(entity.getCreatedBy())
        .customisations(Collections.emptyList())
        .isNew(false)
        .build();
  }

  private static OrderItemCustomisationEntity toEntity(OrderItemCustomisation customisation) {
    if (customisation.isNew()) {
      return OrderItemCustomisationEntity.builder()
          .id(customisation.id())
          .pizzeriaId(customisation.pizzeriaId())
          .orderItemId(customisation.orderItemId())
          .customisationId(customisation.customisationId())
          .customisationNameKey(customisation.customisationNameKey())
          .price(customisation.price())
          .createdAt(customisation.createdAt())
          .isNew(true)
          .build();
    }
    return OrderItemCustomisationEntity.builder()
        .id(customisation.id())
        .pizzeriaId(customisation.pizzeriaId())
        .orderItemId(customisation.orderItemId())
        .customisationId(customisation.customisationId())
        .customisationNameKey(customisation.customisationNameKey())
        .price(customisation.price())
        .createdAt(customisation.createdAt())
        .build();
  }

  private static OrderItemCustomisation toDomain(OrderItemCustomisationEntity entity) {
    return OrderItemCustomisation.builder()
        .id(entity.getId())
        .pizzeriaId(entity.getPizzeriaId())
        .orderItemId(entity.getOrderItemId())
        .customisationId(entity.getCustomisationId())
        .customisationNameKey(entity.getCustomisationNameKey())
        .price(entity.getPrice())
        .createdAt(entity.getCreatedAt())
        .isNew(false)
        .build();
  }
}
