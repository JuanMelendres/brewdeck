import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, render, renderHook, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from './AuthProvider';
import { clearTokens, getRefreshToken, getToken, setRefreshToken, setToken } from './tokenStore';
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
    clearTokens();
    vi.unstubAllGlobals();
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
      emailVerified: true,
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
      refreshToken: 'refresh-jwt',
    });
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 2,
      email: 'a@b.com',
      displayName: null,
      emailVerified: true,
      createdAt: '2026-07-01T00:00:00Z',
    });
    render(<Probe />, { wrapper });
    await userEvent.click(screen.getByRole('button', { name: 'login' }));
    await waitFor(() => expect(screen.getByTestId('email')).toHaveTextContent('a@b.com'));
  });

  it('persists the refresh token after login', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({
      token: 'jwt',
      expiresAt: '2026-07-09T00:00:00Z',
      email: 'a@b.com',
      refreshToken: 'refresh-jwt',
    });
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 2,
      email: 'a@b.com',
      displayName: null,
      emailVerified: true,
      createdAt: '2026-07-01T00:00:00Z',
    });
    render(<Probe />, { wrapper });
    await userEvent.click(screen.getByRole('button', { name: 'login' }));
    await waitFor(() => expect(screen.getByTestId('email')).toHaveTextContent('a@b.com'));
    expect(getRefreshToken()).toBe('refresh-jwt');
  });

  it('resets to anonymous and clears the user on logout', async () => {
    setToken('jwt');
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 3,
      email: 'brewer@example.com',
      displayName: null,
      emailVerified: true,
      createdAt: '2026-07-01T00:00:00Z',
    });
    render(<Probe />, { wrapper });
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));

    await userEvent.click(screen.getByRole('button', { name: 'logout' }));

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
    expect(screen.getByTestId('email')).toHaveTextContent('none');
  });

  it('calls the logout API with the stored refresh token and clears both tokens', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({
      token: 'jwt',
      expiresAt: '2026-07-09T00:00:00Z',
      email: 'a@b.com',
      refreshToken: 'refresh-jwt',
    });
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 2,
      email: 'a@b.com',
      displayName: null,
      emailVerified: true,
      createdAt: '2026-07-01T00:00:00Z',
    });
    const logoutSpy = vi.spyOn(authApi, 'logout').mockResolvedValue(undefined);

    render(<Probe />, { wrapper });
    await userEvent.click(screen.getByRole('button', { name: 'login' }));
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));

    await userEvent.click(screen.getByRole('button', { name: 'logout' }));

    expect(logoutSpy).toHaveBeenCalledWith('refresh-jwt');
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
    expect(getToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
  });

  it('still clears tokens locally when the logout API call rejects', async () => {
    vi.spyOn(authApi, 'login').mockResolvedValue({
      token: 'jwt',
      expiresAt: '2026-07-09T00:00:00Z',
      email: 'a@b.com',
      refreshToken: 'refresh-jwt',
    });
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 2,
      email: 'a@b.com',
      displayName: null,
      emailVerified: true,
      createdAt: '2026-07-01T00:00:00Z',
    });
    vi.spyOn(authApi, 'logout').mockRejectedValue(new Error('Network error'));

    render(<Probe />, { wrapper });
    await userEvent.click(screen.getByRole('button', { name: 'login' }));
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));

    await userEvent.click(screen.getByRole('button', { name: 'logout' }));

    expect(authApi.logout).toHaveBeenCalledWith('refresh-jwt');
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
    expect(getToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
  });

  it('sends the authenticated logout request with the bearer token before clearing storage', async () => {
    // Exercises the REAL apiFetch path (global fetch mocked, not authApi.logout)
    // so a regression that drops the Authorization header cannot slip through.
    setToken('access-jwt');
    setRefreshToken('refresh-jwt');
    vi.spyOn(authApi, 'getMe').mockResolvedValue({
      id: 4,
      email: 'a@b.com',
      displayName: null,
      emailVerified: true,
      createdAt: '2026-07-01T00:00:00Z',
    });
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 204,
      statusText: 'No Content',
      json: () => Promise.resolve(undefined),
    });
    vi.stubGlobal('fetch', fetchMock);

    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.status).toBe('authenticated'));

    await act(async () => {
      await result.current.logout();
    });

    const logoutCall = fetchMock.mock.calls.find(([url]) =>
      String(url).endsWith('/api/auth/logout'),
    );
    expect(logoutCall).toBeDefined();
    const [, requestInit] = logoutCall as [string, RequestInit];
    expect(requestInit.method).toBe('POST');
    expect(requestInit.body).toBe(JSON.stringify({ refreshToken: 'refresh-jwt' }));
    expect((requestInit.headers as Record<string, string>).Authorization).toBe(
      'Bearer access-jwt',
    );

    expect(getToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
    expect(result.current.status).toBe('anonymous');
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
