import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useBrewSessions } from './useBrewSessions';
import * as sessionsApi from '@/lib/api/brewSessions';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useBrewSessions', () => {
  it('returns the paginated brew sessions from the API', async () => {
    vi.spyOn(sessionsApi, 'listBrewSessions').mockResolvedValue({
      content: [
        {
          id: 1, recipeId: 1, recipeName: 'Mezcla AeroPress', brewedAt: '2026-01-01T10:30:00',
          actualGrind: null, actualTemp: 90, actualTime: '2:30', tasteResult: 'Clean',
          rating: 9, adjustmentNotes: null,
        },
      ],
      page: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true,
    });

    const { result } = renderHook(() => useBrewSessions({ page: 0, size: 10 }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.content[0].recipeName).toBe('Mezcla AeroPress');
    expect(result.current.data?.totalElements).toBe(1);
  });
});
