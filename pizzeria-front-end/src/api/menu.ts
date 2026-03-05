import api from './client';
import type { MenuResponse } from '../types/api';

export const fetchMenu = (pizzeriaCode: string) => {
  return api.get<MenuResponse>(`/pizzerias/${pizzeriaCode}/menu`);
};
