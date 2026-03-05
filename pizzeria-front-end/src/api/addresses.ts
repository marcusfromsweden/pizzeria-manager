import api from './client';
import type {
  SaveDeliveryAddressRequest,
  DeliveryAddressResponse,
} from '../types/api';

export const fetchAddresses = () => {
  return api.get<DeliveryAddressResponse[]>('/users/me/addresses');
};

export const saveAddress = (payload: SaveDeliveryAddressRequest) => {
  return api.post<DeliveryAddressResponse>('/users/me/addresses', payload);
};

export const deleteAddress = (addressId: string) => {
  return api.delete<void>(`/users/me/addresses/${addressId}`);
};

export const setDefaultAddress = (addressId: string) => {
  return api.post<DeliveryAddressResponse>(`/users/me/addresses/${addressId}/default`);
};
