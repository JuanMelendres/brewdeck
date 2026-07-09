import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useCoffee } from './useCoffee';
import * as coffeesApi from '@/lib/api/coffees';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

afterEach(() => vi.restoreAllMocks());

describe('useCoffee', () => {
  it('fetches the coffee by id', async () => {
    const spy = vi.spyOn(coffeesApi, 'getCoffee').mockResolvedValue({ id: 3, name: 'Mezcla' } as never);

    const { result } = renderHook(() => useCoffee(3), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(spy).toHaveBeenCalledWith(3);
    expect(result.current.data?.name).toBe('Mezcla');
  });
});
