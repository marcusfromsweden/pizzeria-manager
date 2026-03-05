package com.pizzeriaservice.service.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.pizzeriaservice.api.dto.DeliveryAddressResponse;
import com.pizzeriaservice.api.dto.FeedbackResponse;
import com.pizzeriaservice.api.dto.OrderResponse;
import com.pizzeriaservice.api.dto.OrderSummaryResponse;
import com.pizzeriaservice.api.dto.PizzaScoreResponse;
import com.pizzeriaservice.api.dto.PizzaType;
import com.pizzeriaservice.service.domain.FeedbackKind;
import com.pizzeriaservice.service.domain.FeedbackStatus;
import com.pizzeriaservice.service.domain.FulfillmentType;
import com.pizzeriaservice.service.domain.OrderStatus;
import com.pizzeriaservice.service.domain.PizzaKind;
import com.pizzeriaservice.service.domain.PizzaSize;
import com.pizzeriaservice.service.domain.model.DeliveryAddress;
import com.pizzeriaservice.service.domain.model.Feedback;
import com.pizzeriaservice.service.domain.model.Order;
import com.pizzeriaservice.service.domain.model.OrderItem;
import com.pizzeriaservice.service.domain.model.OrderItemCustomisation;
import com.pizzeriaservice.service.domain.model.PizzaScore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RestDomainConverterTest {

  private static final UUID PIZZERIA_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
  private static final Instant NOW = Instant.parse("2025-06-15T12:00:00Z");
  private static final Instant UPDATED_AT = Instant.parse("2025-06-15T13:00:00Z");

  private RestDomainConverter converter;

  @BeforeEach
  void setUp() {
    converter = new RestDomainConverter();
  }

  @Test
  void shouldConvertFeedbackToResponse() {
    UUID feedbackId = UUID.randomUUID();
    Instant adminRepliedAt = Instant.parse("2025-06-15T14:00:00Z");
    Instant adminReplyReadAt = Instant.parse("2025-06-15T15:00:00Z");

    Feedback feedback =
        Feedback.builder()
            .id(feedbackId)
            .pizzeriaId(PIZZERIA_ID)
            .userId(USER_ID)
            .kind(FeedbackKind.SERVICE)
            .message("Great pizza!")
            .rating(5)
            .category("DELIVERY")
            .status(FeedbackStatus.OPEN)
            .adminReply("Thank you!")
            .adminRepliedAt(adminRepliedAt)
            .adminReplyReadAt(adminReplyReadAt)
            .createdAt(NOW)
            .updatedAt(UPDATED_AT)
            .createdBy(USER_ID)
            .updatedBy(USER_ID)
            .isNew(false)
            .build();

    FeedbackResponse response = converter.toFeedbackResponse(feedback);

    assertThat(response.id()).isEqualTo(feedbackId);
    assertThat(response.userId()).isEqualTo(USER_ID);
    assertThat(response.type()).isEqualTo("SERVICE");
    assertThat(response.message()).isEqualTo("Great pizza!");
    assertThat(response.rating()).isEqualTo(5);
    assertThat(response.category()).isEqualTo("DELIVERY");
    assertThat(response.adminReply()).isEqualTo("Thank you!");
    assertThat(response.adminRepliedAt()).isEqualTo(adminRepliedAt);
    assertThat(response.adminReplyReadAt()).isEqualTo(adminReplyReadAt);
    assertThat(response.createdAt()).isEqualTo(NOW);
  }

  @Test
  void shouldConvertPizzaScoreToResponse() {
    UUID scoreId = UUID.randomUUID();
    UUID pizzaId = UUID.randomUUID();

    PizzaScore pizzaScore =
        PizzaScore.builder()
            .id(scoreId)
            .pizzeriaId(PIZZERIA_ID)
            .userId(USER_ID)
            .pizzaId(pizzaId)
            .pizzaKind(PizzaKind.TEMPLATE)
            .score(4)
            .comment("Very tasty")
            .createdAt(NOW)
            .createdBy(USER_ID)
            .isNew(false)
            .build();

    PizzaScoreResponse response = converter.toPizzaScoreResponse(pizzaScore);

    assertThat(response.id()).isEqualTo(scoreId);
    assertThat(response.userId()).isEqualTo(USER_ID);
    assertThat(response.pizzaId()).isEqualTo(pizzaId);
    assertThat(response.pizzaType()).isEqualTo(PizzaType.TEMPLATE);
    assertThat(response.score()).isEqualTo(4);
    assertThat(response.comment()).isEqualTo("Very tasty");
    assertThat(response.createdAt()).isEqualTo(NOW);
  }

  @Test
  void shouldConvertDeliveryAddressToResponse() {
    UUID addressId = UUID.randomUUID();

    DeliveryAddress address =
        DeliveryAddress.builder()
            .id(addressId)
            .pizzeriaId(PIZZERIA_ID)
            .userId(USER_ID)
            .label("Home")
            .street("Storgatan 1")
            .postalCode("21140")
            .city("Malmo")
            .phone("+46701234567")
            .instructions("Ring the doorbell twice")
            .isDefault(true)
            .createdAt(NOW)
            .updatedAt(UPDATED_AT)
            .isNew(false)
            .build();

    DeliveryAddressResponse response = converter.toDeliveryAddressResponse(address);

    assertThat(response.id()).isEqualTo(addressId);
    assertThat(response.label()).isEqualTo("Home");
    assertThat(response.street()).isEqualTo("Storgatan 1");
    assertThat(response.postalCode()).isEqualTo("21140");
    assertThat(response.city()).isEqualTo("Malmo");
    assertThat(response.phone()).isEqualTo("+46701234567");
    assertThat(response.instructions()).isEqualTo("Ring the doorbell twice");
    assertThat(response.isDefault()).isTrue();
    assertThat(response.createdAt()).isEqualTo(NOW);
  }

  @Test
  void shouldConvertOrderToResponse() {
    UUID orderId = UUID.randomUUID();
    UUID orderItemId = UUID.randomUUID();
    UUID menuItemId = UUID.randomUUID();
    UUID customisationResponseId = UUID.randomUUID();
    UUID customisationId = UUID.randomUUID();
    Instant requestedTime = Instant.parse("2025-06-15T18:00:00Z");
    Instant estimatedReadyTime = Instant.parse("2025-06-15T18:30:00Z");

    OrderItemCustomisation customisation =
        OrderItemCustomisation.builder()
            .id(customisationResponseId)
            .pizzeriaId(PIZZERIA_ID)
            .orderItemId(orderItemId)
            .customisationId(customisationId)
            .customisationNameKey("translation.key.extra.mozzarella")
            .price(new BigDecimal("15.00"))
            .createdAt(NOW)
            .isNew(false)
            .build();

    OrderItem orderItem =
        OrderItem.builder()
            .id(orderItemId)
            .pizzeriaId(PIZZERIA_ID)
            .orderId(orderId)
            .menuItemId(menuItemId)
            .menuItemNameKey("translation.key.disc.pizza.margarita")
            .size(PizzaSize.REGULAR)
            .quantity(2)
            .basePrice(new BigDecimal("95.00"))
            .customisationsPrice(new BigDecimal("15.00"))
            .itemTotal(new BigDecimal("205.00"))
            .specialInstructions("Extra crispy")
            .createdAt(NOW)
            .createdBy(USER_ID)
            .customisations(List.of(customisation))
            .isNew(false)
            .build();

    Order order =
        Order.builder()
            .id(orderId)
            .pizzeriaId(PIZZERIA_ID)
            .userId(USER_ID)
            .orderNumber("ORD-001")
            .status(OrderStatus.PENDING)
            .fulfillmentType(FulfillmentType.DELIVERY)
            .deliveryAddressId(UUID.randomUUID())
            .deliveryStreet("Storgatan 1")
            .deliveryPostalCode("21140")
            .deliveryCity("Malmo")
            .deliveryPhone("+46701234567")
            .deliveryInstructions("Leave at door")
            .requestedTime(requestedTime)
            .estimatedReadyTime(estimatedReadyTime)
            .subtotal(new BigDecimal("205.00"))
            .deliveryFee(new BigDecimal("29.00"))
            .total(new BigDecimal("234.00"))
            .customerNotes("No onions please")
            .createdAt(NOW)
            .updatedAt(UPDATED_AT)
            .createdBy(USER_ID)
            .updatedBy(USER_ID)
            .items(List.of(orderItem))
            .isNew(false)
            .build();

    OrderResponse response = converter.toOrderResponse(order, List.of(orderItem));

    assertThat(response.id()).isEqualTo(orderId);
    assertThat(response.orderNumber()).isEqualTo("ORD-001");
    assertThat(response.status()).isEqualTo("PENDING");
    assertThat(response.fulfillmentType()).isEqualTo("DELIVERY");
    assertThat(response.deliveryStreet()).isEqualTo("Storgatan 1");
    assertThat(response.deliveryPostalCode()).isEqualTo("21140");
    assertThat(response.deliveryCity()).isEqualTo("Malmo");
    assertThat(response.deliveryPhone()).isEqualTo("+46701234567");
    assertThat(response.deliveryInstructions()).isEqualTo("Leave at door");
    assertThat(response.requestedTime()).isEqualTo(requestedTime);
    assertThat(response.estimatedReadyTime()).isEqualTo(estimatedReadyTime);
    assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("205.00"));
    assertThat(response.deliveryFee()).isEqualByComparingTo(new BigDecimal("29.00"));
    assertThat(response.total()).isEqualByComparingTo(new BigDecimal("234.00"));
    assertThat(response.customerNotes()).isEqualTo("No onions please");
    assertThat(response.createdAt()).isEqualTo(NOW);
    assertThat(response.updatedAt()).isEqualTo(UPDATED_AT);

    assertThat(response.items()).hasSize(1);
    var itemResponse = response.items().get(0);
    assertThat(itemResponse.id()).isEqualTo(orderItemId);
    assertThat(itemResponse.menuItemId()).isEqualTo(menuItemId);
    assertThat(itemResponse.menuItemNameKey()).isEqualTo("translation.key.disc.pizza.margarita");
    assertThat(itemResponse.size()).isEqualTo("REGULAR");
    assertThat(itemResponse.quantity()).isEqualTo(2);
    assertThat(itemResponse.basePrice()).isEqualByComparingTo(new BigDecimal("95.00"));
    assertThat(itemResponse.customisationsPrice()).isEqualByComparingTo(new BigDecimal("15.00"));
    assertThat(itemResponse.itemTotal()).isEqualByComparingTo(new BigDecimal("205.00"));
    assertThat(itemResponse.specialInstructions()).isEqualTo("Extra crispy");

    assertThat(itemResponse.customisations()).hasSize(1);
    var custResponse = itemResponse.customisations().get(0);
    assertThat(custResponse.id()).isEqualTo(customisationResponseId);
    assertThat(custResponse.customisationId()).isEqualTo(customisationId);
    assertThat(custResponse.customisationNameKey()).isEqualTo("translation.key.extra.mozzarella");
    assertThat(custResponse.price()).isEqualByComparingTo(new BigDecimal("15.00"));
  }

  @Test
  void shouldConvertOrderToSummaryResponse() {
    UUID orderId = UUID.randomUUID();

    Order order =
        Order.builder()
            .id(orderId)
            .pizzeriaId(PIZZERIA_ID)
            .userId(USER_ID)
            .orderNumber("ORD-042")
            .status(OrderStatus.PENDING)
            .fulfillmentType(FulfillmentType.PICKUP)
            .subtotal(new BigDecimal("190.00"))
            .deliveryFee(BigDecimal.ZERO)
            .total(new BigDecimal("190.00"))
            .createdAt(NOW)
            .updatedAt(UPDATED_AT)
            .createdBy(USER_ID)
            .updatedBy(USER_ID)
            .items(List.of())
            .isNew(false)
            .build();

    OrderSummaryResponse response = converter.toOrderSummaryResponse(order, 3);

    assertThat(response.id()).isEqualTo(orderId);
    assertThat(response.orderNumber()).isEqualTo("ORD-042");
    assertThat(response.status()).isEqualTo("PENDING");
    assertThat(response.fulfillmentType()).isEqualTo("PICKUP");
    assertThat(response.total()).isEqualByComparingTo(new BigDecimal("190.00"));
    assertThat(response.itemCount()).isEqualTo(3);
    assertThat(response.createdAt()).isEqualTo(NOW);
  }

  @Test
  void shouldConvertPizzaTypeToKind() {
    assertThat(RestDomainConverter.fromPizzaType(PizzaType.TEMPLATE)).isEqualTo(PizzaKind.TEMPLATE);
    assertThat(RestDomainConverter.fromPizzaType(PizzaType.CUSTOM)).isEqualTo(PizzaKind.CUSTOM);
  }

  @Test
  void shouldConvertPizzaKindToType() {
    assertThat(RestDomainConverter.toType(PizzaKind.TEMPLATE)).isEqualTo(PizzaType.TEMPLATE);
    assertThat(RestDomainConverter.toType(PizzaKind.CUSTOM)).isEqualTo(PizzaType.CUSTOM);
  }
}
