import { afterEach, describe, expect, it, vi } from 'vitest';
import { fetchFeatureFlags, normalizeFeatureFlags } from './featureFlags';
import * as client from './client';

afterEach(() => vi.restoreAllMocks());

describe('normalizeFeatureFlags', () => {
  it('defaults unknown or missing flags to false', () => {
    expect(normalizeFeatureFlags(undefined)).toEqual({ aiRecipeAssistant: false });
    expect(normalizeFeatureFlags({})).toEqual({ aiRecipeAssistant: false });
  });

  it('passes through explicit values', () => {
    expect(normalizeFeatureFlags({ aiRecipeAssistant: true })).toEqual({ aiRecipeAssistant: true });
  });
});

describe('fetchFeatureFlags', () => {
  it('requests the endpoint and normalizes the features map', async () => {
    const spy = vi
      .spyOn(client, 'apiFetch')
      .mockResolvedValue({ features: { aiRecipeAssistant: true } } as never);

    const flags = await fetchFeatureFlags();

    expect(spy).toHaveBeenCalledWith('/api/feature-flags');
    expect(flags).toEqual({ aiRecipeAssistant: true });
  });
});
