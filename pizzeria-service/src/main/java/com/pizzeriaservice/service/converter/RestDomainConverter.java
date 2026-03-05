package com.pizzeriaservice.service.converter;

import com.pizzeriaservice.api.dto.DeliveryAddressResponse;
import com.pizzeriaservice.api.dto.FeedbackResponse;
import com.pizzeriaservice.api.dto.OrderItemCustomisationResponse;
import com.pizzeriaservice.api.dto.OrderItemResponse;
import com.pizzeriaservice.api.dto.OrderResponse;
import com.pizzeriaservice.api.dto.OrderSummaryResponse;
import com.pizzeriaservice.api.dto.PizzaScoreResponse;
import com.pizzeriaservice.api.dto.PizzaType;
import com.pizzeriaservice.service.domain.PizzaKind;
import com.pizzeriaservice.service.domain.model.DeliveryAddress;
import com.pizzeriaservice.service.domain.model.Feedback;
import com.pizzeriaservice.service.domain.model.Order;
import com.pizzeriaservice.service.domain.model.OrderItem;
import com.pizzeriaservice.service.domain.model.PizzaScore;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RestDomainConverter {

  public FeedbackResponse toFeedbackResponse(Feedback feedback) {
    return new FeedbackResponse(
        feedback.id(),
        feedback.userId(),
        feedback.kind().name(),
        feedback.message(),
        feedback.rating(),
        feedback.category(),
        feedback.adminReply(),
        feedback.adminRepliedAt(),
        feedback.adminReplyReadAt(),
        feedback.createdAt());
  }

  public PizzaScoreResponse toPizzaScoreResponse(PizzaScore score) {
    return new PizzaScoreResponse(
        score.id(),
        score.userId(),
        score.pizzaId(),
        toType(score.pizzaKind()),
        score.score(),
        score.comment(),
        score.createdAt());
  }

  public DeliveryAddressResponse toDeliveryAddressResponse(DeliveryAddress address) {
    return new DeliveryAddressResponse(
        address.id(),
        address.label(),
        address.street(),
        address.postalCode(),
        address.city(),
        address.phone(),
        address.instructions(),
        address.isDefault(),
        address.createdAt());
  }

  public OrderResponse toOrderResponse(Order order, List<OrderItem> items) {
    List<OrderItemResponse> itemResponses =
        items.stream()
            .map(
                item ->
                    new OrderItemResponse(
                        item.id(),
                        item.menuItemId(),
                        item.menuItemNameKey(),
                        item.size().name(),
                        item.quantity(),
                        item.basePrice(),
                        item.customisationsPrice(),
                        item.itemTotal(),
                        item.specialInstructions(),
                        item.customisations().stream()
                            .map(
                                c ->
                                    new OrderItemCustomisationResponse(
                                        c.id(),
                                        c.customisationId(),
                                        c.customisationNameKey(),
                                        c.price()))
                            .collect(Collectors.toList())))
            .collect(Collectors.toList());

    return new OrderResponse(
        order.id(),
        order.orderNumber(),
        order.status().name(),
        order.fulfillmentType().name(),
        order.deliveryStreet(),
        order.deliveryPostalCode(),
        order.deliveryCity(),
        order.deliveryPhone(),
        order.deliveryInstructions(),
        order.requestedTime(),
        order.estimatedReadyTime(),
        order.subtotal(),
        order.deliveryFee(),
        order.total(),
        order.customerNotes(),
        itemResponses,
        order.createdAt(),
        order.updatedAt());
  }

  public OrderSummaryResponse toOrderSummaryResponse(Order order, int itemCount) {
    return new OrderSummaryResponse(
        order.id(),
        order.orderNumber(),
        order.status().name(),
        order.fulfillmentType().name(),
        order.total(),
        itemCount,
        order.createdAt());
  }

  public static PizzaKind fromPizzaType(PizzaType type) {
    return switch (type) {
      case TEMPLATE -> PizzaKind.TEMPLATE;
      case CUSTOM -> PizzaKind.CUSTOM;
    };
  }

  public static PizzaType toType(PizzaKind kind) {
    return switch (kind) {
      case TEMPLATE -> PizzaType.TEMPLATE;
      case CUSTOM -> PizzaType.CUSTOM;
    };
  }
}
