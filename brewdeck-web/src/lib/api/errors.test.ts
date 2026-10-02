import { describe, expect, it } from 'vitest';
import { ApiError } from './client';
import { rateLimitMessage } from './errors';

describe('rateLimitMessage', () => {
  it('returns the server message for a 429', () => {
    const error = new ApiError(429, 'Too many attempts. Try again in 3 minutes.');
    expect(rateLimitMessage(error)).toBe('Too many attempts. Try again in 3 minutes.');
  });

  it('returns null for any other error', () => {
    expect(rateLimitMessage(new ApiError(401, 'Invalid email or password'))).toBeNull();
    expect(rateLimitMessage(new Error('network'))).toBeNull();
    expect(rateLimitMessage(undefined)).toBeNull();
  });
});
