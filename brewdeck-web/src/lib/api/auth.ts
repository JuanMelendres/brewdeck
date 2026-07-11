import { apiFetch } from './client';
import type { AuthResponse, UserResponse } from './types';

export function register(body: { email: string; password: string }): Promise<AuthResponse> {
  return apiFetch<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function login(body: { email: string; password: string }): Promise<AuthResponse> {
  return apiFetch<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function getMe(): Promise<UserResponse> {
  return apiFetch<UserResponse>('/api/auth/me');
}

export function updateProfile(body: { displayName: string | null }): Promise<UserResponse> {
  return apiFetch<UserResponse>('/api/auth/me', {
    method: 'PATCH',
    body: JSON.stringify(body),
  });
}

export function changePassword(body: {
  currentPassword: string;
  newPassword: string;
}): Promise<void> {
  return apiFetch<void>('/api/auth/change-password', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}
