import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import {
  useCreateBrewSession,
  useDeleteBrewSession,
  useUpdateBrewSession,
} from './useBrewSessionMutations';
import * as brewSessionsApi from '@/lib/api/brewSessions';

function setup() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const invalidateSpy = vi.spyOn(client, 'invalidateQueries');
  const removeSpy = vi.spyOn(client, 'removeQueries');
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { wrapper, invalidateSpy, removeSpy };
}

describe('useCreateBrewSession', () => {
  it('invalidates brew sessions, recipes and dashboard on success', async () => {
    vi.spyOn(brewSessionsApi, 'createBrewSession').mockResolvedValue({ id: 1 } as never);
    const { wrapper, invalidateSpy } = setup();

    const { result } = renderHook(() => useCreateBrewSession(), { wrapper });
    result.current.mutate({ recipeId: 1, rating: 9 });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['brew-sessions'] });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['recipes'] });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['dashboard'] });
  });
});

describe('useUpdateBrewSession', () => {
  it('sends the id and body, then invalidates sessions, recipes and dashboard', async () => {
    const update = vi.spyOn(brewSessionsApi, 'updateBrewSession').mockResolvedValue({ id: 5 } as never);
    const { wrapper, invalidateSpy } = setup();

    const { result } = renderHook(() => useUpdateBrewSession(), { wrapper });
    result.current.mutate({ id: 5, body: { recipeId: 1, rating: 7 } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(update).toHaveBeenCalledWith(5, { recipeId: 1, rating: 7 });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['brew-sessions'] });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['recipes'] });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['dashboard'] });
  });
});

describe('useDeleteBrewSession', () => {
  it('drops the deleted detail entry and invalidates the lists', async () => {
    vi.spyOn(brewSessionsApi, 'deleteBrewSession').mockResolvedValue(undefined);
    const { wrapper, invalidateSpy, removeSpy } = setup();

    const { result } = renderHook(() => useDeleteBrewSession(), { wrapper });
    result.current.mutate(5);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(removeSpy).toHaveBeenCalledWith({ queryKey: ['brew-sessions', 'detail', 5] });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['brew-sessions'] });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['dashboard'] });
  });
});
