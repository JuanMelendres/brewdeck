import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useCreateRecipe, useUpdateRecipe, useDeleteRecipe } from './useRecipeMutations';
import * as recipesApi from '@/lib/api/recipes';

function setup() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const invalidateSpy = vi.spyOn(client, 'invalidateQueries');
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { wrapper, invalidateSpy };
}

describe('recipe mutation hooks', () => {
  it('useCreateRecipe invalidates recipes on success', async () => {
    vi.spyOn(recipesApi, 'createRecipe').mockResolvedValue({ id: 1, name: 'AeroPress' } as never);
    const { wrapper, invalidateSpy } = setup();
    const { result } = renderHook(() => useCreateRecipe(), { wrapper });
    result.current.mutate({ coffeeId: 1, methodId: 2, name: 'AeroPress' });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['recipes'] });
  });

  it('useUpdateRecipe invalidates recipes on success', async () => {
    vi.spyOn(recipesApi, 'updateRecipe').mockResolvedValue({ id: 1, name: 'Updated' } as never);
    const { wrapper, invalidateSpy } = setup();
    const { result } = renderHook(() => useUpdateRecipe(), { wrapper });
    result.current.mutate({ id: 1, body: { coffeeId: 1, methodId: 2, name: 'Updated' } });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['recipes'] });
  });

  it('useDeleteRecipe invalidates recipes on success', async () => {
    vi.spyOn(recipesApi, 'deleteRecipe').mockResolvedValue(undefined);
    const { wrapper, invalidateSpy } = setup();
    const { result } = renderHook(() => useDeleteRecipe(), { wrapper });
    result.current.mutate(1);
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['recipes'] });
  });
});
