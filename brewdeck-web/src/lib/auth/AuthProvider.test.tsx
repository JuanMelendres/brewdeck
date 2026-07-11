import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from './AuthProvider';
import { clearToken, getToken, setToken } from './tokenStore';
import * as authApi from '@/lib/api/auth';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient();
  return (
    <QueryClientProvider client={client}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  );
}

function Probe() {
  const { status, user, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="status">{status}</span>
      <span data-testid="email">{user?.email ?? 'none'}</span>
      <button onClick={() => login({ email: 'a@b.com', password: 'password1' })}>login</button>
      <button onClick={logout}>logout</button>
    </div>
  );
}

describe('AuthProvider', () => {
  afterEach(() => {
    clearToken();
    vi.restoreAllMocks();
  });

  it('is anonymous when no token exists', async () => {
    render(<Probe />, { wrapper });
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
  });

  it('hydrates the user from getMe when a token exists', async () => {
    setToken('jwt');
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 1,
      email: 'brewer@example.com',
      displayName: null,
      createdAt: '2026-07-01T00:00:00Z',
    });
    render(<Probe />, { wrapper });
    await waitFor(() => expect(screen.getByTestId('email')).toHaveTextContent('brewer@example.com'));
    expect(screen.getByTestId('status')).toHaveTextContent('authenticated');
  });

  it('logs in and stores the token', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({
      token: 'jwt',
      expiresAt: '2026-07-09T00:00:00Z',
      email: 'a@b.com',
    });
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 2,
      email: 'a@b.com',
      displayName: null,
      createdAt: '2026-07-01T00:00:00Z',
    });
    render(<Probe />, { wrapper });
    await userEvent.click(screen.getByRole('button', { name: 'login' }));
    await waitFor(() => expect(screen.getByTestId('email')).toHaveTextContent('a@b.com'));
  });

  it('resets to anonymous and clears the user on logout', async () => {
    setToken('jwt');
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 3,
      email: 'brewer@example.com',
      displayName: null,
      createdAt: '2026-07-01T00:00:00Z',
    });
    render(<Probe />, { wrapper });
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));

    await userEvent.click(screen.getByRole('button', { name: 'logout' }));

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
    expect(screen.getByTestId('email')).toHaveTextContent('none');
  });

  it('falls back to anonymous when getMe rejects during hydration', async () => {
    setToken('jwt');
    vi.spyOn(authApi, 'getMe').mockRejectedValue(new Error('Unauthorized'));
    render(<Probe />, { wrapper });

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
    expect(screen.getByTestId('email')).toHaveTextContent('none');
    expect(getToken()).toBeNull();
  });
});
