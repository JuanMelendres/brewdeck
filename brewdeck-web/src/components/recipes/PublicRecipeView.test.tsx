import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { PublicRecipeView } from './PublicRecipeView';
import * as hook from '@/hooks/usePublicRecipe';
import type { PublicRecipe } from '@/lib/api/types';

function mockHook(value: Partial<ReturnType<typeof hook.usePublicRecipe>>) {
  vi.spyOn(hook, 'usePublicRecipe').mockReturnValue(
    value as ReturnType<typeof hook.usePublicRecipe>,
  );
}

const sample: PublicRecipe = {
  name: 'Morning Cup',
  coffeeName: 'Ethiopia',
  methodName: 'V60',
  coffeeGrams: 15,
  waterGrams: 250,
  ratio: '1:16',
  grindSetting: 'Medium',
  waterTemp: 94,
  brewTime: '3:00',
  steps: 'Bloom then pour',
  expectedTaste: 'Floral',
};

describe('PublicRecipeView', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, data: undefined, isError: false });
    render(<PublicRecipeView token="tok-1" />);
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('shows the unavailable empty state on error (404)', () => {
    mockHook({ isLoading: false, data: undefined, isError: true });
    render(<PublicRecipeView token="tok-1" />);
    expect(screen.getByText(/isn't available/i)).toBeInTheDocument();
  });

  it('renders the recipe card on success', () => {
    mockHook({ isLoading: false, data: sample, isError: false });
    render(<PublicRecipeView token="tok-1" />);
    expect(screen.getByRole('heading', { name: /morning cup/i })).toBeInTheDocument();
    expect(screen.getByText('Ethiopia')).toBeInTheDocument();
    expect(screen.getByText('V60')).toBeInTheDocument();
  });
});
