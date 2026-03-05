package com.pizzeriaservice.service.service;

import com.pizzeriaservice.api.dto.CreateOrderRequest;
import com.pizzeriaservice.api.dto.OrderItemRequest;
import com.pizzeriaservice.api.dto.OrderResponse;
import com.pizzeriaservice.api.dto.OrderSummaryResponse;
import com.pizzeriaservice.service.converter.RestDomainConverter;
import com.pizzeriaservice.service.domain.FulfillmentType;
import com.pizzeriaservice.service.domain.OrderStatus;
import com.pizzeriaservice.service.domain.PizzaSize;
import com.pizzeriaservice.service.domain.model.Order;
import com.pizzeriaservice.service.domain.model.OrderItem;
import com.pizzeriaservice.service.domain.model.OrderItemCustomisation;
import com.pizzeriaservice.service.menu.MenuItemEntity;
import com.pizzeriaservice.service.menu.MenuItemRepository;
import com.pizzeriaservice.service.menu.PizzaCustomisationEntity;
import com.pizzeriaservice.service.menu.PizzaCustomisationRepository;
import com.pizzeriaservice.service.repository.OrderRepository;
import com.pizzeriaservice.service.support.OrderValidator;
import com.pizzeriaservice.service.support.TimeProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrderService {

  private static final BigDecimal DEFAULT_DELIVERY_FEE = new BigDecimal("49.00");
  private static final DateTimeFormatter ORDER_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd");

  private final OrderRepository orderRepository;
  private final MenuItemRepository menuItemRepository;
  private final PizzaCustomisationRepository customisationRepository;
  private final TimeProvider timeProvider;
  private final RestDomainConverter converter;
  private final OrderValidator orderValidator;

  public Mono<OrderResponse> createOrder(UUID userId, UUID pizzeriaId, CreateOrderRequest request) {
    FulfillmentType fulfillmentType = FulfillmentType.valueOf(request.fulfillmentType());

    Set<UUID> menuItemIds =
        request.items().stream().map(OrderItemRequest::menuItemId).collect(Collectors.toSet());

    Set<UUID> customisationIds =
        request.items().stream()
            .filter(item -> item.customisationIds() != null)
            .flatMap(item -> item.customisationIds().stream())
            .collect(Collectors.toSet());

    Mono<Map<UUID, MenuItemEntity>> menuItemsMono =
        Flux.fromIterable(menuItemIds)
            .flatMap(id -> menuItemRepository.findByIdAndPizzeriaId(id, pizzeriaId))
            .collectMap(MenuItemEntity::id);

    Mono<Map<UUID, PizzaCustomisationEntity>> customisationsMono =
        customisationIds.isEmpty()
            ? Mono.just(Collections.emptyMap())
            : Flux.fromIterable(customisationIds)
                .flatMap(customisationRepository::findById)
                .collectMap(PizzaCustomisationEntity::id);

    return Mono.zip(menuItemsMono, customisationsMono)
        .flatMap(
            tuple -> {
              Map<UUID, MenuItemEntity> menuItems = tuple.getT1();
              Map<UUID, PizzaCustomisationEntity> customisations = tuple.getT2();

              orderValidator.validateOrderItems(request, menuItems, customisations);

              return generateOrderNumber(pizzeriaId)
                  .flatMap(
                      orderNumber ->
                          createOrderWithItems(
                              userId,
                              pizzeriaId,
                              orderNumber,
                              request,
                              fulfillmentType,
                              menuItems,
                              customisations));
            });
  }

  private Mono<String> generateOrderNumber(UUID pizzeriaId) {
    String dateStr =
        LocalDate.ofInstant(timeProvider.now(), ZoneId.systemDefault()).format(ORDER_DATE_FORMAT);
    String prefix = "RM-" + dateStr + "-";
    return orderRepository
        .findMaxOrderNumberForPrefix(pizzeriaId, prefix)
        .defaultIfEmpty(0)
        .map(max -> prefix + String.format("%03d", max + 1));
  }

  private Mono<OrderResponse> createOrderWithItems(
      UUID userId,
      UUID pizzeriaId,
      String orderNumber,
      CreateOrderRequest request,
      FulfillmentType fulfillmentType,
      Map<UUID, MenuItemEntity> menuItems,
      Map<UUID, PizzaCustomisationEntity> customisations) {

    UUID orderId = UUID.randomUUID();
    List<OrderItemData> itemDataList = new ArrayList<>();
    BigDecimal subtotal = BigDecimal.ZERO;

    for (OrderItemRequest itemRequest : request.items()) {
      MenuItemEntity menuItem = menuItems.get(itemRequest.menuItemId());
      PizzaSize size = PizzaSize.valueOf(itemRequest.size());

      BigDecimal basePrice =
          size == PizzaSize.FAMILY ? menuItem.priceFamily() : menuItem.priceRegular();
      BigDecimal customisationsPrice = BigDecimal.ZERO;

      List<CustomisationData> custDataList = new ArrayList<>();
      if (itemRequest.customisationIds() != null) {
        for (UUID custId : itemRequest.customisationIds()) {
          PizzaCustomisationEntity cust = customisations.get(custId);
          BigDecimal custPrice =
              size == PizzaSize.FAMILY ? cust.priceFamily() : cust.priceRegular();
          customisationsPrice = customisationsPrice.add(custPrice);
          custDataList.add(new CustomisationData(custId, cust.nameKey(), custPrice));
        }
      }

      BigDecimal itemTotal =
          basePrice.add(customisationsPrice).multiply(BigDecimal.valueOf(itemRequest.quantity()));
      subtotal = subtotal.add(itemTotal);

      itemDataList.add(
          new OrderItemData(
              UUID.randomUUID(),
              itemRequest.menuItemId(),
              menuItem.nameKey(),
              size,
              itemRequest.quantity(),
              basePrice,
              customisationsPrice,
              itemTotal,
              itemRequest.specialInstructions(),
              custDataList));
    }

    BigDecimal deliveryFee =
        fulfillmentType == FulfillmentType.DELIVERY ? DEFAULT_DELIVERY_FEE : BigDecimal.ZERO;
    BigDecimal total = subtotal.add(deliveryFee);

    Order order =
        Order.builder()
            .id(orderId)
            .pizzeriaId(pizzeriaId)
            .userId(userId)
            .orderNumber(orderNumber)
            .status(OrderStatus.PENDING)
            .fulfillmentType(fulfillmentType)
            .deliveryAddressId(request.deliveryAddressId())
            .deliveryStreet(request.deliveryStreet())
            .deliveryPostalCode(request.deliveryPostalCode())
            .deliveryCity(request.deliveryCity())
            .deliveryPhone(request.deliveryPhone())
            .deliveryInstructions(request.deliveryInstructions())
            .requestedTime(request.requestedTime())
            .estimatedReadyTime(null)
            .subtotal(subtotal)
            .deliveryFee(deliveryFee)
            .total(total)
            .customerNotes(request.customerNotes())
            .createdAt(timeProvider.now())
            .updatedAt(timeProvider.now())
            .createdBy(userId)
            .updatedBy(userId)
            .items(Collections.emptyList())
            .isNew(true)
            .build();

    return orderRepository
        .save(order)
        .flatMap(savedOrder -> saveOrderItems(savedOrder, pizzeriaId, itemDataList));
  }

  private Mono<OrderResponse> saveOrderItems(
      Order order, UUID pizzeriaId, List<OrderItemData> itemDataList) {
    return Flux.fromIterable(itemDataList)
        .flatMap(
            itemData -> {
              OrderItem item =
                  OrderItem.builder()
                      .id(itemData.id)
                      .pizzeriaId(pizzeriaId)
                      .orderId(order.id())
                      .menuItemId(itemData.menuItemId)
                      .menuItemNameKey(itemData.menuItemNameKey)
                      .size(itemData.size)
                      .quantity(itemData.quantity)
                      .basePrice(itemData.basePrice)
                      .customisationsPrice(itemData.customisationsPrice)
                      .itemTotal(itemData.itemTotal)
                      .specialInstructions(itemData.specialInstructions)
                      .createdAt(timeProvider.now())
                      .createdBy(order.userId())
                      .customisations(Collections.emptyList())
                      .isNew(true)
                      .build();

              return orderRepository
                  .saveOrderItem(item)
                  .flatMap(
                      savedItem ->
                          saveCustomisations(savedItem, pizzeriaId, itemData.customisations)
                              .collectList()
                              .map(custs -> savedItem.toBuilder().customisations(custs).build()));
            })
        .collectList()
        .map(items -> converter.toOrderResponse(order, items));
  }

  private Flux<OrderItemCustomisation> saveCustomisations(
      OrderItem item, UUID pizzeriaId, List<CustomisationData> customisations) {
    return Flux.fromIterable(customisations)
        .flatMap(
            custData -> {
              OrderItemCustomisation cust =
                  OrderItemCustomisation.builder()
                      .id(UUID.randomUUID())
                      .pizzeriaId(pizzeriaId)
                      .orderItemId(item.id())
                      .customisationId(custData.customisationId)
                      .customisationNameKey(custData.nameKey)
                      .price(custData.price)
                      .createdAt(timeProvider.now())
                      .isNew(true)
                      .build();
              return orderRepository.saveOrderItemCustomisation(cust);
            });
  }

  public Flux<OrderSummaryResponse> getOrderHistory(UUID userId, UUID pizzeriaId) {
    return orderRepository
        .findByUserIdAndPizzeriaId(userId, pizzeriaId)
        .flatMap(this::enrichWithItemCount);
  }

  public Flux<OrderSummaryResponse> getActiveOrders(UUID userId, UUID pizzeriaId) {
    return orderRepository
        .findActiveByUserIdAndPizzeriaId(userId, pizzeriaId)
        .flatMap(this::enrichWithItemCount);
  }

  private Mono<OrderSummaryResponse> enrichWithItemCount(Order order) {
    return orderRepository
        .findItemsByOrderId(order.id())
        .count()
        .map(count -> converter.toOrderSummaryResponse(order, count.intValue()));
  }

  public Mono<OrderResponse> getOrder(UUID orderId, UUID userId, UUID pizzeriaId) {
    return orderRepository
        .findByIdAndUserIdAndPizzeriaId(orderId, userId, pizzeriaId)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found")))
        .flatMap(this::enrichOrderWithItems);
  }

  private Mono<OrderResponse> enrichOrderWithItems(Order order) {
    return orderRepository
        .findItemsByOrderId(order.id())
        .collectList()
        .flatMap(
            items -> {
              if (items.isEmpty()) {
                return Mono.just(converter.toOrderResponse(order, items));
              }
              List<UUID> itemIds = items.stream().map(OrderItem::id).collect(Collectors.toList());
              return orderRepository
                  .findCustomisationsByOrderItemIds(itemIds)
                  .collectList()
                  .map(
                      customisations -> {
                        Map<UUID, List<OrderItemCustomisation>> custByItem =
                            customisations.stream()
                                .collect(
                                    Collectors.groupingBy(OrderItemCustomisation::orderItemId));
                        List<OrderItem> enrichedItems =
                            items.stream()
                                .map(
                                    item ->
                                        item.toBuilder()
                                            .customisations(
                                                custByItem.getOrDefault(
                                                    item.id(), Collections.emptyList()))
                                            .build())
                                .collect(Collectors.toList());
                        return converter.toOrderResponse(order, enrichedItems);
                      });
            });
  }

  public Mono<OrderResponse> cancelOrder(UUID orderId, UUID userId, UUID pizzeriaId) {
    return orderRepository
        .findByIdAndUserIdAndPizzeriaId(orderId, userId, pizzeriaId)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found")))
        .flatMap(
            order -> {
              if (order.status() != OrderStatus.PENDING
                  && order.status() != OrderStatus.CONFIRMED) {
                return Mono.error(
                    new IllegalStateException(
                        "Order cannot be cancelled in status: " + order.status()));
              }
              Order updated =
                  order.toBuilder()
                      .status(OrderStatus.CANCELLED)
                      .updatedAt(timeProvider.now())
                      .updatedBy(userId)
                      .isNew(false)
                      .build();
              return orderRepository.save(updated);
            })
        .flatMap(this::enrichOrderWithItems);
  }

  private record OrderItemData(
      UUID id,
      UUID menuItemId,
      String menuItemNameKey,
      PizzaSize size,
      int quantity,
      BigDecimal basePrice,
      BigDecimal customisationsPrice,
      BigDecimal itemTotal,
      String specialInstructions,
      List<CustomisationData> customisations) {}

  private record CustomisationData(UUID customisationId, String nameKey, BigDecimal price) {}
}
