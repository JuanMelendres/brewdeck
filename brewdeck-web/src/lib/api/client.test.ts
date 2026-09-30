import { afterEach, describe, expect, it, vi } from 'vitest';
import { onEmailNotVerified } from '@/lib/auth/emailVerificationSignal';
import { ApiError, apiFetch, refreshSession } from './client';
import { clearTokens, getToken, setToken } from '@/lib/auth/tokenStore';

function mockFetchOnce(body: unknown, init: { ok: boolean; status: number }) {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      ok: init.ok,
      status: init.status,
      statusText: 'Status',
      json: () => Promise.resolve(body),
    }),
  );
}

function routedFetch(
  handlers: Record<string, () => { ok: boolean; status: number; body: unknown }>,
) {
  return vi.fn((url: string) => {
    const key = Object.keys(handlers).find((k) => String(url).includes(k));
    const res = key ? handlers[key]() : { ok: false, status: 404, body: {} };
    return Promise.resolve({
      ok: res.ok,
      status: res.status,
      statusText: 'Status',
      json: () => Promise.resolve(res.body),
    });
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
  clearTokens();
});

describe('apiFetch', () => {
  it('returns parsed JSON on a 2xx response', async () => {
    mockFetchOnce({ value: 42 }, { ok: true, status: 200 });

    const result = await apiFetch<{ value: number }>('/api/thing');

    expect(result).toEqual({ value: 42 });
  });

  it('throws ApiError with the backend message on a non-2xx response', async () => {
    mockFetchOnce(
      {
        status: 400,
        error: 'Bad Request',
        message: 'Malformed request body',
        path: '/api/thing',
      },
      { ok: false, status: 400 },
    );

    await expect(apiFetch('/api/thing')).rejects.toMatchObject({
      name: 'ApiError',
      status: 400,
      message: 'Malformed request body',
      path: '/api/thing',
    });

    await expect(apiFetch('/api/thing')).rejects.toBeInstanceOf(ApiError);
  });

  it('carries the error code and signals EMAIL_NOT_VERIFIED to the UI', async () => {
    const listener = vi.fn();
    const unsubscribe = onEmailNotVerified(listener);
    mockFetchOnce(
      {
        status: 403,
        error: 'Forbidden',
        message: 'Verify your email address to continue',
        path: '/api/coffees',
        code: 'EMAIL_NOT_VERIFIED',
      },
      { ok: false, status: 403 },
    );

    await expect(apiFetch('/api/coffees')).rejects.toMatchObject({
      status: 403,
      code: 'EMAIL_NOT_VERIFIED',
    });
    expect(listener).toHaveBeenCalledTimes(1);
    unsubscribe();
  });

  it('does not signal for an ordinary 403', async () => {
    const listener = vi.fn();
    const unsubscribe = onEmailNotVerified(listener);
    mockFetchOnce(
      { status: 403, error: 'Forbidden', message: 'Insufficient permissions', path: '/api/admin/x' },
      { ok: false, status: 403 },
    );

    await expect(apiFetch('/api/admin/x')).rejects.toMatchObject({ status: 403 });
    expect(listener).not.toHaveBeenCalled();
    unsubscribe();
  });

  it('adds the Authorization header when a token is present', async () => {
    setToken('jwt-token');
    const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      status: 200,
      statusText: 'OK',
      json: () => Promise.resolve({ ok: true }),
    } as unknown as Response);

    await apiFetch('/api/coffees');

    const init = fetchSpy.mock.calls[0][1];
    expect((init?.headers as Record<string, string>).Authorization).toBe('Bearer jwt-token');
  });

  it('omits the Authorization header when no token is present', async () => {
    clearTokens();
    const fetchSpy = vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: true,
      status: 200,
      statusText: 'OK',
      json: () => Promise.resolve({ ok: true }),
    } as unknown as Response);

    await apiFetch('/api/coffees');

    const init = fetchSpy.mock.calls[0][1];
    expect((init?.headers as Record<string, string>).Authorization).toBeUndefined();
  });

  it('clears the token and redirects to /login on 401', async () => {
    setToken('jwt-token');
    const assignMock = vi.fn();
    vi.stubGlobal('location', { pathname: '/', assign: assignMock });
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      json: () => Promise.resolve({ message: 'Authentication required' }),
    } as unknown as Response);

    await expect(apiFetch('/api/coffees')).rejects.toThrow();
    expect(getToken()).toBeNull();
    expect(assignMock).toHaveBeenCalledWith('/login');
  });

  it('does NOT redirect on 401 when already on /login', async () => {
    setToken('jwt-token');
    const assignMock = vi.fn();
    vi.stubGlobal('location', { pathname: '/login', assign: assignMock });
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      json: () => Promise.resolve({ message: 'Authentication required' }),
    } as unknown as Response);

    await expect(apiFetch('/api/coffees')).rejects.toThrow();
    expect(getToken()).toBeNull();
    expect(assignMock).not.toHaveBeenCalled();
  });

  it('does NOT redirect on 401 when on a /share/* path', async () => {
    setToken('jwt-token');
    const assignMock = vi.fn();
    vi.stubGlobal('location', { pathname: '/share/xyz', assign: assignMock });
    vi.spyOn(global, 'fetch').mockResolvedValue({
      ok: false,
      status: 401,
      statusText: 'Unauthorized',
      json: () => Promise.resolve({ message: 'Authentication required' }),
    } as unknown as Response);

    await expect(apiFetch('/api/coffees')).rejects.toThrow();
    expect(getToken()).toBeNull();
    expect(assignMock).not.toHaveBeenCalled();
  });

  it('refreshes once and retries the original request on 401', async () => {
    setToken('stale-access');
    let coffeesCalls = 0;
    const fetchMock = routedFetch({
      '/api/auth/refresh': () => ({
        ok: true,
        status: 200,
        body: { token: 'fresh-access', email: 'u@e.com', expiresAt: 'x' },
      }),
      '/api/coffees': () => {
        coffeesCalls += 1;
        return coffeesCalls === 1
          ? { ok: false, status: 401, body: { message: 'expired' } }
          : { ok: true, status: 200, body: { value: 1 } };
      },
    });
    vi.stubGlobal('fetch', fetchMock);

    const result = await apiFetch<{ value: number }>('/api/coffees');

    expect(result).toEqual({ value: 1 });
    expect(getToken()).toBe('fresh-access');
    const refreshCalls = fetchMock.mock.calls.filter((c) => String(c[0]).includes('/api/auth/refresh'));
    expect(refreshCalls).toHaveLength(1);
  });

  it('shares a single refresh across concurrent 401s', async () => {
    setToken('stale-access');
    const okAfterRefresh: Record<string, number> = {};
    const fetchMock = routedFetch({
      '/api/auth/refresh': () => ({
        ok: true,
        status: 200,
        body: { token: 'fresh-access', email: 'u@e.com', expiresAt: 'x' },
      }),
      '/api/a': () => {
        okAfterRefresh.a = (okAfterRefresh.a ?? 0) + 1;
        return okAfterRefresh.a === 1
          ? { ok: false, status: 401, body: {} }
          : { ok: true, status: 200, body: { ok: 'a' } };
      },
      '/api/b': () => {
        okAfterRefresh.b = (okAfterRefresh.b ?? 0) + 1;
        return okAfterRefresh.b === 1
          ? { ok: false, status: 401, body: {} }
          : { ok: true, status: 200, body: { ok: 'b' } };
      },
    });
    vi.stubGlobal('fetch', fetchMock);

    await Promise.all([apiFetch('/api/a'), apiFetch('/api/b')]);

    const refreshCalls = fetchMock.mock.calls.filter((c) => String(c[0]).includes('/api/auth/refresh'));
    expect(refreshCalls).toHaveLength(1);
  });

  it('propagates the retried request error (400) without a forced logout when refresh succeeds', async () => {
    setToken('stale-access');
    const assignMock = vi.fn();
    vi.stubGlobal('location', { pathname: '/', assign: assignMock });
    let coffeesCalls = 0;
    vi.stubGlobal(
      'fetch',
      routedFetch({
        '/api/auth/refresh': () => ({
          ok: true,
          status: 200,
          body: { token: 'fresh-access', email: 'u@e.com', expiresAt: 'x' },
        }),
        '/api/coffees': () => {
          coffeesCalls += 1;
          return coffeesCalls === 1
            ? { ok: false, status: 401, body: { message: 'expired' } }
            : { ok: false, status: 400, body: { message: 'Validation failed' } };
        },
      }),
    );

    await expect(apiFetch('/api/coffees')).rejects.toMatchObject({
      name: 'ApiError',
      status: 400,
      message: 'Validation failed',
    });
    expect(assignMock).not.toHaveBeenCalled();
  });

  it('clears tokens and redirects when the refresh itself fails', async () => {
    setToken('stale-access');
    const assignMock = vi.fn();
    vi.stubGlobal('location', { pathname: '/', assign: assignMock });
    vi.stubGlobal(
      'fetch',
      routedFetch({
        '/api/auth/refresh': () => ({ ok: false, status: 401, body: {} }),
        '/api/coffees': () => ({ ok: false, status: 401, body: { message: 'expired' } }),
      }),
    );

    await expect(apiFetch('/api/coffees')).rejects.toThrow();
    expect(getToken()).toBeNull();
    expect(assignMock).toHaveBeenCalledWith('/login');
  });

  it('refreshes with the httpOnly cookie: no body, CSRF header, same-origin credentials', async () => {
    const fetchMock = routedFetch({
      '/api/auth/refresh': () => ({
        ok: true,
        status: 200,
        body: { token: 'fresh-access', email: 'u@e.com', expiresAt: 'x' },
      }),
    });
    vi.stubGlobal('fetch', fetchMock);

    await refreshSession();

    const [, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(init.method).toBe('POST');
    expect(init.body).toBeUndefined();
    expect(init.credentials).toBe('same-origin');
    expect(init.headers).toMatchObject({ 'X-Requested-With': 'fetch' });
    expect(getToken()).toBe('fresh-access');
  });

  it('sends the CSRF header on every request', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({}),
    });
    vi.stubGlobal('fetch', fetchMock);

    await apiFetch('/api/coffees');

    const [, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(init.headers).toMatchObject({ 'X-Requested-With': 'fetch' });
  });

  it('serializes refresh across tabs with a Web Lock when available', async () => {
    const request = vi.fn((_name: string, callback: () => Promise<string>) => callback());
    vi.stubGlobal('navigator', { locks: { request } });
    vi.stubGlobal(
      'fetch',
      routedFetch({
        '/api/auth/refresh': () => ({
          ok: true,
          status: 200,
          body: { token: 'locked-access', email: 'u@e.com', expiresAt: 'x' },
        }),
      }),
    );

    await expect(refreshSession()).resolves.toBe('locked-access');
    expect(request).toHaveBeenCalledWith('brewdeck-refresh', expect.any(Function));
  });

  it('does not try to refresh on a 401 when there is no session (e.g. a failed login)', async () => {
    const fetchMock = routedFetch({
      '/api/auth/login': () => ({ ok: false, status: 401, body: { message: 'Invalid email or password' } }),
    });
    vi.stubGlobal('fetch', fetchMock);
    vi.stubGlobal('location', { pathname: '/login', assign: vi.fn() });

    await expect(apiFetch('/api/auth/login', { method: 'POST' })).rejects.toMatchObject({
      status: 401,
      message: 'Invalid email or password',
    });
    const refreshCalls = fetchMock.mock.calls.filter((c) => String(c[0]).includes('/api/auth/refresh'));
    expect(refreshCalls).toHaveLength(0);
  });
});
