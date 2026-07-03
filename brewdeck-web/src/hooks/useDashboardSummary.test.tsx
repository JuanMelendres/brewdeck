import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useDashboardSummary } from './useDashboardSummary';
import * as dashboardApi from '@/lib/api/dashboard';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('useDashboardSummary', () => {
  it('returns the dashboard summary from the API', async () => {
    vi.spyOn(dashboardApi, 'getDashboardSummary').mockResolvedValue({
      totalCoffees: 5,
      totalBrewMethods: 4,
      totalRecipes: 10,
      favoriteRecipes: 3,
      totalBrewSessions: 20,
      averageSessionRating: 4.25,
    });

    const { result } = renderHook(() => useDashboardSummary(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.totalCoffees).toBe(5);
  });
});
