import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiFetch } from './client';
import { clearToken, getToken, setToken } from '@/lib/auth/tokenStore';

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

afterEach(() => {
  vi.unstubAllGlobals();
  clearToken();
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
});
