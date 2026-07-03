import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiFetch } from './client';

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

afterEach(() => vi.unstubAllGlobals());

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
});
