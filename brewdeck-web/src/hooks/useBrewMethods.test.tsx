import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useBrewMethods } from './useBrewMethods';
import * as brewMethodsApi from '@/lib/api/brewMethods';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

afterEach(() => vi.restoreAllMocks());

describe('useBrewMethods', () => {
  it('fetches brew methods with the given params', async () => {
    const spy = vi.spyOn(brewMethodsApi, 'listBrewMethods').mockResolvedValue({
      content: [{ id: 1, name: 'AeroPress', description: null, createdAt: '2026-01-01', updatedAt: null }],
      page: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true,
    });

    const { result } = renderHook(() => useBrewMethods({ page: 0, size: 10 }), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(spy).toHaveBeenCalledWith({ page: 0, size: 10 });
    expect(result.current.data?.content[0].name).toBe('AeroPress');
  });
});
