import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiFetch } from './client';
import {
  clearRefreshToken,
  clearToken,
  getRefreshToken,
  getToken,
  setRefreshToken,
  setToken,
} from '@/lib/auth/tokenStore';

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
  clearToken();
  clearRefreshToken();
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
    clearToken();
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
    setRefreshToken('r-1');
    let coffeesCalls = 0;
    const fetchMock = routedFetch({
      '/api/auth/refresh': () => ({
        ok: true,
        status: 200,
        body: { token: 'fresh-access', refreshToken: 'r-2', email: 'u@e.com', expiresAt: 'x' },
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
    setRefreshToken('r-1');
    const okAfterRefresh: Record<string, number> = {};
    const fetchMock = routedFetch({
      '/api/auth/refresh': () => ({
        ok: true,
        status: 200,
        body: { token: 'fresh-access', refreshToken: 'r-2', email: 'u@e.com', expiresAt: 'x' },
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

  it('clears tokens and redirects when the refresh itself fails', async () => {
    setToken('stale-access');
    setRefreshToken('r-1');
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
    expect(getRefreshToken()).toBeNull();
    expect(assignMock).toHaveBeenCalledWith('/login');
  });
});
