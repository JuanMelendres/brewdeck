import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useCoffeeOptions } from './useResourceOptions';
import * as coffeesApi from '@/lib/api/coffees';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useCoffeeOptions', () => {
  it('maps the coffee page content to { id, name } options', async () => {
    vi.spyOn(coffeesApi, 'listCoffees').mockResolvedValue({
      content: [{ id: 3, name: 'Mezcla' }] as never,
      page: 0, size: 100, totalElements: 1, totalPages: 1, first: true, last: true,
    });
    const { result } = renderHook(() => useCoffeeOptions(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toEqual([{ id: 3, name: 'Mezcla' }]);
  });
});
