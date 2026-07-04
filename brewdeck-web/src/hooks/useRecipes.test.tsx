import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useRecipes } from './useRecipes';
import * as recipesApi from '@/lib/api/recipes';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useRecipes', () => {
  it('returns the paginated recipes from the API', async () => {
    vi.spyOn(recipesApi, 'listRecipes').mockResolvedValue({
      content: [
        {
          id: 1, coffeeId: 1, coffeeName: 'Mezcla', methodId: 1, methodName: 'AeroPress',
          name: 'Mezcla AeroPress', coffeeGrams: 15, waterGrams: 230, ratio: '1:15',
          grindSetting: null, waterTemp: 90, brewTime: null, steps: null, expectedTaste: null,
          favorite: true, createdAt: '2026-01-01T00:00:00', updatedAt: null,
        },
      ],
      page: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true,
    });

    const { result } = renderHook(() => useRecipes({ page: 0, size: 10 }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.content[0].name).toBe('Mezcla AeroPress');
    expect(result.current.data?.totalElements).toBe(1);
  });
});
