import api from './client';
import type {
  UserRegisterRequest,
  UserRegisterResponse,
  UserLoginRequest,
  UserLoginResponse,
  UserVerifyEmailRequest,
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  ResetPasswordRequest,
  UserProfileResponse,
  UserProfileUpdateRequest,
} from '../types/api';

// Public endpoints (require pizzeriaCode)

export const register = (
  pizzeriaCode: string,
  payload: UserRegisterRequest
) => {
  return api.post<UserRegisterResponse>(
    `/pizzerias/${pizzeriaCode}/users/register`,
    payload
  );
};

export const verifyEmail = (
  pizzeriaCode: string,
  payload: UserVerifyEmailRequest
) => {
  return api.post<void>(`/pizzerias/${pizzeriaCode}/users/verify-email`, payload);
};

export const login = (pizzeriaCode: string, payload: UserLoginRequest) => {
  return api.post<UserLoginResponse>(
    `/pizzerias/${pizzeriaCode}/users/login`,
    payload
  );
};

export const forgotPassword = (
  pizzeriaCode: string,
  payload: ForgotPasswordRequest
) => {
  return api.post<ForgotPasswordResponse>(
    `/pizzerias/${pizzeriaCode}/users/forgot-password`,
    payload
  );
};

export const resetPassword = (
  pizzeriaCode: string,
  payload: ResetPasswordRequest
) => {
  return api.post<void>(`/pizzerias/${pizzeriaCode}/users/reset-password`, payload);
};

// Authenticated endpoints (no pizzeriaCode needed)

export const logout = () => {
  return api.post<void>('/users/logout');
};

export const fetchProfile = () => {
  return api.get<UserProfileResponse>('/users/me');
};

export const updateProfile = (payload: UserProfileUpdateRequest) => {
  return api.patch<UserProfileResponse>('/users/me', payload);
};

export const deleteProfile = () => {
  return api.delete<void>('/users/me');
};
