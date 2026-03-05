import api from './client';
import type { PizzaScoreCreateRequest, PizzaScoreResponse } from '../types/api';

export const createScore = (payload: PizzaScoreCreateRequest) => {
  return api.post<PizzaScoreResponse>('/pizza-scores', payload);
};

export const fetchMyScores = () => {
  return api.get<PizzaScoreResponse[]>('/pizza-scores/me');
};
