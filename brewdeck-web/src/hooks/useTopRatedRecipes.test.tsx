import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useTopRatedRecipes } from './useTopRatedRecipes';
import * as recipesApi from '@/lib/api/recipes';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

afterEach(() => vi.restoreAllMocks());

describe('useTopRatedRecipes', () => {
  it('fetches the top-rated recipes with the given limit', async () => {
    const spy = vi.spyOn(recipesApi, 'listTopRatedRecipes').mockResolvedValue([
      { recipeId: 2, recipeName: 'Best', averageRating: 9, totalSessions: 4 },
    ]);

    const { result } = renderHook(() => useTopRatedRecipes(5), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(spy).toHaveBeenCalledWith(5);
    expect(result.current.data?.[0].recipeName).toBe('Best');
  });
});
