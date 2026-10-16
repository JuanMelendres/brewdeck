import { afterEach, describe, expect, it, vi } from 'vitest';
import robots from './robots';

describe('robots', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('disallows every path by default', () => {
    vi.stubEnv('ALLOW_INDEXING', '');

    expect(robots().rules).toEqual({ userAgent: '*', disallow: '/' });
  });

  it('allows pages but not the API when indexing is enabled', () => {
    vi.stubEnv('ALLOW_INDEXING', 'true');

    expect(robots().rules).toEqual({ userAgent: '*', allow: '/', disallow: '/api/' });
  });
});
