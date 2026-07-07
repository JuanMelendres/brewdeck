import { afterEach, describe, expect, it, vi } from 'vitest';
import { getPublicRecipe } from './publicRecipes';
import * as client from './client';

describe('getPublicRecipe', () => {
  afterEach(() => vi.restoreAllMocks());

  it('fetches the public recipe endpoint with the encoded token', async () => {
    const body = { name: 'Morning Cup', coffeeName: 'Ethiopia', methodName: 'V60' };
    const spy = vi.spyOn(client, 'apiFetch').mockResolvedValue(body as never);

    const result = await getPublicRecipe('tok 1');

    expect(spy).toHaveBeenCalledWith('/api/public/recipes/tok%201');
    expect(result).toEqual(body);
  });
});
