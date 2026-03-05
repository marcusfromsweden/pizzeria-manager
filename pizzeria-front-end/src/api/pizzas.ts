import api from './client';
import type {
  PizzaSummaryResponse,
  PizzaDetailResponse,
  PizzaSuitabilityRequest,
  PizzaSuitabilityResponse,
} from '../types/api';

// Public endpoints (require pizzeriaCode)

export const fetchPizzas = (pizzeriaCode: string) => {
  return api.get<PizzaSummaryResponse[]>(`/pizzerias/${pizzeriaCode}/pizzas`);
};

export const fetchPizza = (pizzeriaCode: string, pizzaId: string) => {
  return api.get<PizzaDetailResponse>(
    `/pizzerias/${pizzeriaCode}/pizzas/${pizzaId}`
  );
};

// Authenticated endpoints

export const checkSuitability = (payload: PizzaSuitabilityRequest) => {
  return api.post<PizzaSuitabilityResponse>('/pizzas/suitability', payload);
};
