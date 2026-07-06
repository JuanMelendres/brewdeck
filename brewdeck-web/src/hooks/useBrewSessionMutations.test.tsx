import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { useCreateBrewSession } from './useBrewSessionMutations';
import * as brewSessionsApi from '@/lib/api/brewSessions';

function setup() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const invalidateSpy = vi.spyOn(client, 'invalidateQueries');
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  );
  return { wrapper, invalidateSpy };
}

describe('useCreateBrewSession', () => {
  it('invalidates brew sessions, recipes and dashboard on success', async () => {
    vi.spyOn(brewSessionsApi, 'createBrewSession').mockResolvedValue({ id: 1 } as never);
    const { wrapper, invalidateSpy } = setup();

    const { result } = renderHook(() => useCreateBrewSession(), { wrapper });
    result.current.mutate({ recipeId: 1, rating: 9 });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['brew-sessions'] });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['recipes'] });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['dashboard'] });
  });
});
