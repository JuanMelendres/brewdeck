import { afterEach, describe, expect, it } from 'vitest';
import { clearTokens, getToken, purgeLegacyTokenStorage, setToken } from './tokenStore';

describe('tokenStore', () => {
  afterEach(() => {
    clearTokens();
    window.localStorage.clear();
  });

  it('holds the access token in memory only', () => {
    expect(getToken()).toBeNull();

    setToken('abc.def.ghi');

    expect(getToken()).toBe('abc.def.ghi');
    expect(window.localStorage.length).toBe(0);
  });

  it('clearTokens forgets the access token', () => {
    setToken('abc.def.ghi');
    clearTokens();
    expect(getToken()).toBeNull();
  });

  it('purges tokens an older version left in localStorage', () => {
    window.localStorage.setItem('brewdeck.token', 'old-access');
    window.localStorage.setItem('brewdeck.refreshToken', 'old-refresh');
    window.localStorage.setItem('unrelated', 'keep');

    purgeLegacyTokenStorage();

    expect(window.localStorage.getItem('brewdeck.token')).toBeNull();
    expect(window.localStorage.getItem('brewdeck.refreshToken')).toBeNull();
    expect(window.localStorage.getItem('unrelated')).toBe('keep');
  });
});
