import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, render, renderHook, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from './AuthProvider';
import { clearTokens, getToken, setToken } from './tokenStore';
import * as authApi from '@/lib/api/auth';
import * as client from '@/lib/api/client';
import type { UserResponse } from '@/lib/api/types';

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient();
  return (
    <QueryClientProvider client={queryClient}>
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

const me = (email: string): UserResponse => ({
  id: 1,
  email,
  displayName: null,
  emailVerified: true,
  role: 'USER',
  themePreference: null, language: null,
  createdAt: '2026-07-01T00:00:00Z',
});

/** No refresh cookie (or an invalid one): the server answers 401. */
function noSession() {
  return vi.spyOn(client, 'refreshSession').mockRejectedValue(new client.ApiError(401, 'x'));
}

/** A valid refresh cookie: the server issues a fresh access token. */
function cookieSession(token = 'cookie-access') {
  return vi.spyOn(client, 'refreshSession').mockImplementation(async () => {
    setToken(token);
    return token;
  });
}

function loginSucceeds() {
  vi.spyOn(authApi, 'login').mockResolvedValue({
    token: 'jwt',
    expiresAt: '2026-07-09T00:00:00Z',
    email: 'a@b.com',
  });
  vi.spyOn(authApi, 'getMe').mockResolvedValue(me('a@b.com'));
}

describe('AuthProvider', () => {
  afterEach(() => {
    clearTokens();
    window.localStorage.clear();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('is anonymous when there is no refresh cookie', async () => {
    noSession();
    render(<Probe />, { wrapper });
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
  });

  it('restores the session from the refresh cookie on load', async () => {
    const refresh = cookieSession();
    vi.spyOn(authApi, 'getMe').mockResolvedValue(me('brewer@example.com'));

    render(<Probe />, { wrapper });

    await waitFor(() => expect(screen.getByTestId('email')).toHaveTextContent('brewer@example.com'));
    expect(screen.getByTestId('status')).toHaveTextContent('authenticated');
    expect(refresh).toHaveBeenCalled();
    expect(getToken()).toBe('cookie-access');
  });

  it('removes tokens an older version left in localStorage', async () => {
    window.localStorage.setItem('brewdeck.refreshToken', 'old-refresh');
    noSession();

    render(<Probe />, { wrapper });

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
    expect(window.localStorage.getItem('brewdeck.refreshToken')).toBeNull();
  });

  it('logs in and keeps the access token in memory only', async () => {
    noSession();
    loginSucceeds();
    render(<Probe />, { wrapper });
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));

    await userEvent.click(screen.getByRole('button', { name: 'login' }));

    await waitFor(() => expect(screen.getByTestId('email')).toHaveTextContent('a@b.com'));
    expect(getToken()).toBe('jwt');
    expect(window.localStorage.length).toBe(0);
  });

  it('resets to anonymous, calls the logout API, and forgets the token on logout', async () => {
    cookieSession();
    vi.spyOn(authApi, 'getMe').mockResolvedValue(me('brewer@example.com'));
    const logoutSpy = vi.spyOn(authApi, 'logout').mockResolvedValue(undefined);
    render(<Probe />, { wrapper });
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));

    await userEvent.click(screen.getByRole('button', { name: 'logout' }));

    expect(logoutSpy).toHaveBeenCalledWith();
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
    expect(screen.getByTestId('email')).toHaveTextContent('none');
    expect(getToken()).toBeNull();
  });

  it('still signs out locally when the logout API call rejects', async () => {
    cookieSession();
    vi.spyOn(authApi, 'getMe').mockResolvedValue(me('brewer@example.com'));
    vi.spyOn(authApi, 'logout').mockRejectedValue(new Error('Network error'));
    render(<Probe />, { wrapper });
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'));

    await userEvent.click(screen.getByRole('button', { name: 'logout' }));

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
    expect(getToken()).toBeNull();
  });

  it('sends logout with the bearer token and CSRF header, and no body', async () => {
    // Exercises the REAL apiFetch path (global fetch mocked) so a regression that drops the
    // Authorization or X-Requested-With header cannot slip through.
    cookieSession('access-jwt');
    vi.spyOn(authApi, 'getMe').mockResolvedValue(me('a@b.com'));
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
    expect(requestInit.body).toBeUndefined();
    expect(requestInit.headers).toMatchObject({
      Authorization: 'Bearer access-jwt',
      'X-Requested-With': 'fetch',
    });
    expect(getToken()).toBeNull();
    expect(result.current.status).toBe('anonymous');
  });

  it('falls back to anonymous when getMe rejects after a refresh', async () => {
    cookieSession();
    vi.spyOn(authApi, 'getMe').mockRejectedValue(new Error('Unauthorized'));
    render(<Probe />, { wrapper });

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'));
    expect(getToken()).toBeNull();
  });
});
