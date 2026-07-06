import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useFavoriteRecipes } from './useFavoriteRecipes';
import * as recipesApi from '@/lib/api/recipes';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

afterEach(() => vi.restoreAllMocks());

describe('useFavoriteRecipes', () => {
  it('fetches favorite recipes with the given params', async () => {
    const spy = vi.spyOn(recipesApi, 'listFavoriteRecipes').mockResolvedValue({
      content: [{ id: 1, name: 'Fav' }] as never,
      page: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true,
    });

    const { result } = renderHook(() => useFavoriteRecipes({ page: 0, size: 10 }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(spy).toHaveBeenCalledWith({ page: 0, size: 10 });
    expect(result.current.data?.content[0].name).toBe('Fav');
  });
});
