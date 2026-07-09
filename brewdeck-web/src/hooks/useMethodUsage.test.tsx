import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useMethodUsage } from './useMethodUsage';
import * as brewMethodsApi from '@/lib/api/brewMethods';

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

afterEach(() => vi.restoreAllMocks());

describe('useMethodUsage', () => {
  it('fetches the method usage breakdown', async () => {
    const spy = vi.spyOn(brewMethodsApi, 'listMethodUsage').mockResolvedValue([
      { methodId: 1, methodName: 'AeroPress', recipeCount: 5 },
    ]);

    const { result } = renderHook(() => useMethodUsage(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(spy).toHaveBeenCalled();
    expect(result.current.data?.[0].methodName).toBe('AeroPress');
  });
});
