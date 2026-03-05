import api from './client';
import type {
  DietPreferenceResponse,
  DietPreferenceUpdateRequest,
  IngredientIdResponse,
  PreferredIngredientRequest,
} from '../types/api';

// Diet preference

export const fetchDiet = () => {
  return api.get<DietPreferenceResponse>('/users/me/diet');
};

export const updateDiet = (payload: DietPreferenceUpdateRequest) => {
  return api.put<DietPreferenceResponse>('/users/me/diet', payload);
};

// Preferred ingredients

export const fetchPreferredIngredients = () => {
  return api.get<IngredientIdResponse[]>(
    '/users/me/preferences/ingredients/preferred'
  );
};

export const addPreferredIngredient = (payload: PreferredIngredientRequest) => {
  return api.post<void>('/users/me/preferences/ingredients/preferred', payload);
};

export const removePreferredIngredient = (ingredientId: string) => {
  return api.delete<void>(
    `/users/me/preferences/ingredients/preferred/${ingredientId}`
  );
};
