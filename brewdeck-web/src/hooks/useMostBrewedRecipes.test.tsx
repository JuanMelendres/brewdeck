import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useMostBrewedRecipes } from './useMostBrewedRecipes';
import * as recipesApi from '@/lib/api/recipes';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

afterEach(() => vi.restoreAllMocks());

describe('useMostBrewedRecipes', () => {
  it('fetches the most-brewed recipes with the given limit', async () => {
    const spy = vi.spyOn(recipesApi, 'listMostBrewedRecipes').mockResolvedValue([
      { recipeId: 2, recipeName: 'Busy', totalSessions: 9 },
    ]);

    const { result } = renderHook(() => useMostBrewedRecipes(5), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(spy).toHaveBeenCalledWith(5);
    expect(result.current.data?.[0].recipeName).toBe('Busy');
  });
});
