import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { FeatureFlagProvider, useFeatureFlag } from './FeatureFlagProvider';
import * as authProvider from '@/lib/auth/AuthProvider';
import * as featureFlagsApi from '@/lib/api/featureFlags';

type AuthValue = ReturnType<typeof authProvider.useAuth>;

function mockAuth(status: AuthValue['status'], userId: number | null) {
  vi.spyOn(authProvider, 'useAuth').mockReturnValue({
    user: userId ? ({ id: userId } as AuthValue['user']) : null,
    status,
  } as AuthValue);
}

function Consumer() {
  const enabled = useFeatureFlag('aiRecipeAssistant');
  return <span>ai:{String(enabled)}</span>;
}

function renderProvider() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <FeatureFlagProvider>
        <Consumer />
      </FeatureFlagProvider>
    </QueryClientProvider>,
  );
}

afterEach(() => vi.restoreAllMocks());

describe('FeatureFlagProvider', () => {
  it('exposes enabled flags once the API resolves', async () => {
    mockAuth('authenticated', 7);
    vi.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue({ aiRecipeAssistant: true });

    renderProvider();

    await waitFor(() => expect(screen.getByText('ai:true')).toBeInTheDocument());
  });

  it('defaults every flag to disabled while loading (no flash of unfinished UI)', () => {
    mockAuth('authenticated', 7);
    // Never resolves: the provider must report disabled meanwhile.
    vi.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockReturnValue(new Promise(() => {}));

    renderProvider();

    expect(screen.getByText('ai:false')).toBeInTheDocument();
  });

  it('fails safe to disabled when the API errors', async () => {
    mockAuth('authenticated', 7);
    vi.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockRejectedValue(new Error('boom'));

    renderProvider();

    // Stays disabled; give the rejected query a tick to settle.
    await waitFor(() => expect(screen.getByText('ai:false')).toBeInTheDocument());
  });

  it('does not call the API for anonymous users', () => {
    mockAuth('anonymous', null);
    const spy = vi.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue({
      aiRecipeAssistant: true,
    });

    renderProvider();

    expect(spy).not.toHaveBeenCalled();
    expect(screen.getByText('ai:false')).toBeInTheDocument();
  });
});
