import { afterEach, describe, expect, it } from 'vitest';
import {
  clearRefreshToken,
  clearToken,
  clearTokens,
  getRefreshToken,
  getToken,
  setRefreshToken,
  setToken,
} from './tokenStore';

describe('tokenStore', () => {
  afterEach(() => clearTokens());

  it('returns null when no token is set', () => {
    expect(getToken()).toBeNull();
  });

  it('stores and reads a token', () => {
    setToken('abc.def.ghi');
    expect(getToken()).toBe('abc.def.ghi');
  });

  it('clears a stored token', () => {
    setToken('abc.def.ghi');
    clearToken();
    expect(getToken()).toBeNull();
  });

  it('stores, reads, and clears the refresh token', () => {
    setRefreshToken('r-1');
    expect(getRefreshToken()).toBe('r-1');
    clearRefreshToken();
    expect(getRefreshToken()).toBeNull();
  });

  it('clearTokens clears both the access and refresh tokens', () => {
    setToken('a-1');
    setRefreshToken('r-1');
    clearTokens();
    expect(getToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
  });
});
