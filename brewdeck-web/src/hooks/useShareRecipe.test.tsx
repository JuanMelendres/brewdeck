import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useShareRecipe, useUnshareRecipe } from './useShareRecipe';
import * as recipesApi from '@/lib/api/recipes';
import { keys } from '@/lib/query/keys';

function wrapper(client: QueryClient) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
  };
}

describe('useShareRecipe / useUnshareRecipe', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shares and invalidates the recipe detail query', async () => {
    const client = new QueryClient();
    const invalidate = vi.spyOn(client, 'invalidateQueries');
    vi.spyOn(recipesApi, 'shareRecipe').mockResolvedValue({ id: 1, shareToken: 'tok' } as never);

    const { result } = renderHook(() => useShareRecipe(1), { wrapper: wrapper(client) });
    await act(async () => {
      await result.current.mutateAsync();
    });

    await waitFor(() =>
      expect(invalidate).toHaveBeenCalledWith({ queryKey: keys.recipes.detail(1) }),
    );
  });

  it('unshares and invalidates the recipe detail query', async () => {
    const client = new QueryClient();
    const invalidate = vi.spyOn(client, 'invalidateQueries');
    vi.spyOn(recipesApi, 'unshareRecipe').mockResolvedValue({ id: 1, shareToken: null } as never);

    const { result } = renderHook(() => useUnshareRecipe(1), { wrapper: wrapper(client) });
    await act(async () => {
      await result.current.mutateAsync();
    });

    await waitFor(() =>
      expect(invalidate).toHaveBeenCalledWith({ queryKey: keys.recipes.detail(1) }),
    );
  });
});
