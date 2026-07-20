import { screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { FeatureRouteGuard } from './FeatureRouteGuard';
import * as provider from '@/lib/featureFlags/FeatureFlagProvider';

const { replaceMock } = vi.hoisted(() => ({ replaceMock: vi.fn() }));

vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: replaceMock }),
}));

type FlagsValue = ReturnType<typeof provider.useFeatureFlags>;

function mockFlags(status: FlagsValue['status'], aiRecipeAssistant: boolean) {
  vi.spyOn(provider, 'useFeatureFlags').mockReturnValue({
    flags: { aiRecipeAssistant },
    status,
  });
}

afterEach(() => {
  vi.restoreAllMocks();
  replaceMock.mockReset();
});

describe('FeatureRouteGuard', () => {
  it('shows a spinner while flags load, without rendering the page', () => {
    mockFlags('loading', false);

    renderWithTheme(
      <FeatureRouteGuard name="aiRecipeAssistant">
        <span>Secret page</span>
      </FeatureRouteGuard>,
    );

    expect(screen.getByRole('progressbar')).toBeInTheDocument();
    expect(screen.queryByText('Secret page')).not.toBeInTheDocument();
    expect(replaceMock).not.toHaveBeenCalled();
  });

  it('renders children when the feature is enabled', () => {
    mockFlags('ready', true);

    renderWithTheme(
      <FeatureRouteGuard name="aiRecipeAssistant">
        <span>Secret page</span>
      </FeatureRouteGuard>,
    );

    expect(screen.getByText('Secret page')).toBeInTheDocument();
    expect(replaceMock).not.toHaveBeenCalled();
  });

  it('redirects on direct access when the feature is disabled', async () => {
    mockFlags('ready', false);

    renderWithTheme(
      <FeatureRouteGuard name="aiRecipeAssistant" redirectTo="/dashboard">
        <span>Secret page</span>
      </FeatureRouteGuard>,
    );

    expect(screen.queryByText('Secret page')).not.toBeInTheDocument();
    await waitFor(() => expect(replaceMock).toHaveBeenCalledWith('/dashboard'));
  });
});
