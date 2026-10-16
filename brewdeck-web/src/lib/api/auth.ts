import { apiFetch } from './client';
import type { AuthResponse, Language, ThemePreference, UserResponse } from './types';

/**
 * Answers the same for new and existing emails and creates no session (AUTH-05.7): the user
 * confirms by email, then logs in.
 */
export function register(body: { email: string; password: string }): Promise<{ message: string }> {
  return apiFetch<{ message: string }>('/api/auth/register', {
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

export function updateLanguage(body: { language: Language }): Promise<UserResponse> {
  return apiFetch<UserResponse>('/api/auth/me/language', {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

export function updateTheme(body: { themePreference: ThemePreference }): Promise<UserResponse> {
  return apiFetch<UserResponse>('/api/auth/me/theme', {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

/**
 * Signs out every other session and returns a fresh access token for this one (the server also
 * rotates the refresh cookie). The access token sent with this request stops working.
 */
export function changePassword(body: {
  currentPassword: string;
  newPassword: string;
}): Promise<AuthResponse> {
  return apiFetch<AuthResponse>('/api/auth/change-password', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function forgotPassword(body: { email: string }): Promise<{ message: string }> {
  return apiFetch<{ message: string }>('/api/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function resetPassword(body: { token: string; newPassword: string }): Promise<void> {
  return apiFetch<void>('/api/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function verifyEmail(token: string): Promise<void> {
  return apiFetch<void>('/api/auth/verify-email', {
    method: 'POST',
    body: JSON.stringify({ token }),
  });
}

export function resendVerification(): Promise<{ message: string }> {
  return apiFetch<{ message: string }>('/api/auth/resend-verification', {
    method: 'POST',
  });
}

/** Revokes the refresh cookie's token server-side and clears the cookie (ADR-013). */
export function logout(): Promise<void> {
  return apiFetch<void>('/api/auth/logout', { method: 'POST' });
}
