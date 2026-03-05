package com.pizzeriaservice.service.domain;

public enum OrderStatus {
  PENDING,
  CONFIRMED,
  PREPARING,
  READY,
  OUT_FOR_DELIVERY,
  DELIVERED,
  PICKED_UP,
  CANCELLED
}
