import { describe, expect, it } from 'vitest';
import { isIndexingAllowed } from './indexing';

describe('isIndexingAllowed', () => {
  it('is off unless ALLOW_INDEXING is exactly "true"', () => {
    expect(isIndexingAllowed({})).toBe(false);
    expect(isIndexingAllowed({ ALLOW_INDEXING: 'false' })).toBe(false);
    expect(isIndexingAllowed({ ALLOW_INDEXING: '1' })).toBe(false);
    expect(isIndexingAllowed({ ALLOW_INDEXING: 'true' })).toBe(true);
  });
});
