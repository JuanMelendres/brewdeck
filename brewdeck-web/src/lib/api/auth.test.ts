import { afterEach, describe, expect, it, vi } from 'vitest';
import { getMe, login, logout, register } from './auth';
import * as client from './client';

describe('auth api', () => {
  afterEach(() => vi.restoreAllMocks());

  it('POSTs registration', async () => {
    const body = { token: 't', expiresAt: '2026-07-09T00:00:00Z', email: 'a@b.com' };
    const spy = vi.spyOn(client, 'apiFetch').mockResolvedValue(body as never);

    const result = await register({ email: 'a@b.com', password: 'password1' });

    expect(spy).toHaveBeenCalledWith('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ email: 'a@b.com', password: 'password1' }),
    });
    expect(result).toEqual(body);
  });

  it('POSTs login', async () => {
    const body = { token: 't', expiresAt: '2026-07-09T00:00:00Z', email: 'a@b.com' };
    const spy = vi.spyOn(client, 'apiFetch').mockResolvedValue(body as never);

    await login({ email: 'a@b.com', password: 'password1' });

    expect(spy).toHaveBeenCalledWith('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: 'a@b.com', password: 'password1' }),
    });
  });

  it('GETs the current user', async () => {
    const spy = vi
      .spyOn(client, 'apiFetch')
      .mockResolvedValue({ id: 1, email: 'a@b.com', createdAt: '2026-07-01T00:00:00Z' } as never);

    await getMe();

    expect(spy).toHaveBeenCalledWith('/api/auth/me');
  });

  it('logout posts to /api/auth/logout without a body (the cookie identifies the session)', async () => {
    const spy = vi.spyOn(client, 'apiFetch').mockResolvedValue(undefined as never);

    await logout();

    expect(spy).toHaveBeenCalledWith('/api/auth/logout', { method: 'POST' });
  });
});
