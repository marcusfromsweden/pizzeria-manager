import api from './client';
import type { PizzeriaInfoResponse } from '../types/api';

export const fetchPizzeriaInfo = (pizzeriaCode: string) => {
  return api.get<PizzeriaInfoResponse>(`/pizzerias/${pizzeriaCode}`);
};
