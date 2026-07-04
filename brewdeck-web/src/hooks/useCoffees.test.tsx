import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useCoffees } from './useCoffees';
import * as coffeesApi from '@/lib/api/coffees';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useCoffees', () => {
  it('returns the paginated coffees from the API', async () => {
    vi.spyOn(coffeesApi, 'listCoffees').mockResolvedValue({
      content: [
        {
          id: 1, name: 'Mezcla', brand: null, origin: 'Veracruz', region: null, farm: null,
          producer: null, variety: null, process: null, roastLevel: null, notesPrimary: null,
          notesSecondary: null, acidity: null, body: null, sweetness: null, bitterness: null,
          description: null, createdAt: '2026-01-01T00:00:00', updatedAt: null,
        },
      ],
      page: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true,
    });

    const { result } = renderHook(() => useCoffees({ page: 0, size: 10 }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.content[0].name).toBe('Mezcla');
    expect(result.current.data?.totalElements).toBe(1);
  });
});
