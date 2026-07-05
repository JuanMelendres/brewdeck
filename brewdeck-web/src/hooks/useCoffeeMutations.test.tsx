import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useCreateCoffee, useDeleteCoffee, useUpdateCoffee } from './useCoffeeMutations';
import * as coffeesApi from '@/lib/api/coffees';

function setup() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const invalidateSpy = vi.spyOn(client, 'invalidateQueries');
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { wrapper, invalidateSpy };
}

describe('coffee mutation hooks', () => {
  it('useCreateCoffee invalidates the coffees list on success', async () => {
    vi.spyOn(coffeesApi, 'createCoffee').mockResolvedValue({ id: 1, name: 'Mezcla' } as never);
    const { wrapper, invalidateSpy } = setup();

    const { result } = renderHook(() => useCreateCoffee(), { wrapper });
    result.current.mutate({ name: 'Mezcla' });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['coffees'] });
  });

  it('useUpdateCoffee invalidates the coffees list on success', async () => {
    vi.spyOn(coffeesApi, 'updateCoffee').mockResolvedValue({ id: 1, name: 'Updated' } as never);
    const { wrapper, invalidateSpy } = setup();

    const { result } = renderHook(() => useUpdateCoffee(), { wrapper });
    result.current.mutate({ id: 1, body: { name: 'Updated' } });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['coffees'] });
  });

  it('useDeleteCoffee invalidates the coffees list on success', async () => {
    vi.spyOn(coffeesApi, 'deleteCoffee').mockResolvedValue(undefined);
    const { wrapper, invalidateSpy } = setup();

    const { result } = renderHook(() => useDeleteCoffee(), { wrapper });
    result.current.mutate(1);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['coffees'] });
  });
});
