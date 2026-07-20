import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { FeatureFlag } from './FeatureFlag';
import * as provider from '@/lib/featureFlags/FeatureFlagProvider';

afterEach(() => vi.restoreAllMocks());

describe('FeatureFlag', () => {
  it('renders children when the feature is enabled', () => {
    vi.spyOn(provider, 'useFeatureFlag').mockReturnValue(true);

    renderWithTheme(
      <FeatureFlag name="aiRecipeAssistant">
        <span>AI panel</span>
      </FeatureFlag>,
    );

    expect(screen.getByText('AI panel')).toBeInTheDocument();
  });

  it('renders nothing when the feature is disabled and no fallback is given', () => {
    vi.spyOn(provider, 'useFeatureFlag').mockReturnValue(false);

    renderWithTheme(
      <FeatureFlag name="aiRecipeAssistant">
        <span>AI panel</span>
      </FeatureFlag>,
    );

    expect(screen.queryByText('AI panel')).not.toBeInTheDocument();
  });

  it('renders the fallback when the feature is disabled', () => {
    vi.spyOn(provider, 'useFeatureFlag').mockReturnValue(false);

    renderWithTheme(
      <FeatureFlag name="aiRecipeAssistant" fallback={<span>Coming soon</span>}>
        <span>AI panel</span>
      </FeatureFlag>,
    );

    expect(screen.queryByText('AI panel')).not.toBeInTheDocument();
    expect(screen.getByText('Coming soon')).toBeInTheDocument();
  });
});
