import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useMostUsedCoffees } from './useMostUsedCoffees';
import * as coffeesApi from '@/lib/api/coffees';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

afterEach(() => vi.restoreAllMocks());

describe('useMostUsedCoffees', () => {
  it('fetches the most-used coffees with the given limit', async () => {
    const spy = vi.spyOn(coffeesApi, 'listMostUsedCoffees').mockResolvedValue([
      { coffeeId: 2, coffeeName: 'Popular', recipeCount: 7 },
    ]);

    const { result } = renderHook(() => useMostUsedCoffees(5), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(spy).toHaveBeenCalledWith(5);
    expect(result.current.data?.[0].coffeeName).toBe('Popular');
  });
});
