import api from './client';
import type {
  CreateOrderRequest,
  OrderResponse,
  OrderSummaryResponse,
} from '../types/api';

export const createOrder = (payload: CreateOrderRequest) => {
  return api.post<OrderResponse>('/orders', payload);
};

export const fetchOrderHistory = () => {
  return api.get<OrderSummaryResponse[]>('/orders');
};

export const fetchActiveOrders = () => {
  return api.get<OrderSummaryResponse[]>('/orders/active');
};

export const fetchOrder = (orderId: string) => {
  return api.get<OrderResponse>(`/orders/${orderId}`);
};

export const cancelOrder = (orderId: string) => {
  return api.post<OrderResponse>(`/orders/${orderId}/cancel`);
};
