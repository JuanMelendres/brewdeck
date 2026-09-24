import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  useCreateBrewMethod,
  useDeleteBrewMethod,
  useUpdateBrewMethod,
} from './useBrewMethodMutations';
import * as methodsApi from '@/lib/api/brewMethods';

function setup() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const invalidateSpy = vi.spyOn(client, 'invalidateQueries');
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { wrapper, invalidateSpy };
}

function expectMethodQueriesInvalidated(invalidateSpy: ReturnType<typeof setup>['invalidateSpy']) {
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['brew-methods'] });
  expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['dashboard'] });
}

afterEach(() => vi.restoreAllMocks());

describe('brew method mutation hooks', () => {
  it('useCreateBrewMethod invalidates brew-method and dashboard queries on success', async () => {
    vi.spyOn(methodsApi, 'createBrewMethod').mockResolvedValue({ id: 1, name: 'Mine' } as never);
    const { wrapper, invalidateSpy } = setup();

    const { result } = renderHook(() => useCreateBrewMethod(), { wrapper });
    result.current.mutate({ name: 'Mine' });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expectMethodQueriesInvalidated(invalidateSpy);
  });

  it('useUpdateBrewMethod invalidates brew-method and dashboard queries on success', async () => {
    vi.spyOn(methodsApi, 'updateBrewMethod').mockResolvedValue({ id: 1, name: 'Renamed' } as never);
    const { wrapper, invalidateSpy } = setup();

    const { result } = renderHook(() => useUpdateBrewMethod(), { wrapper });
    result.current.mutate({ id: 1, body: { name: 'Renamed' } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expectMethodQueriesInvalidated(invalidateSpy);
  });

  it('useDeleteBrewMethod invalidates brew-method and dashboard queries on success', async () => {
    vi.spyOn(methodsApi, 'deleteBrewMethod').mockResolvedValue(undefined);
    const { wrapper, invalidateSpy } = setup();

    const { result } = renderHook(() => useDeleteBrewMethod(), { wrapper });
    result.current.mutate(1);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expectMethodQueriesInvalidated(invalidateSpy);
  });
});
