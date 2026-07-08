import { afterEach, describe, expect, it } from 'vitest';
import { clearToken, getToken, setToken } from './tokenStore';

describe('tokenStore', () => {
  afterEach(() => clearToken());

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
});
