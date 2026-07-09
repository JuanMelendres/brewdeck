import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { MostBrewedRecipes } from './MostBrewedRecipes';
import * as hook from '@/hooks/useMostBrewedRecipes';
import type { MostBrewedRecipe } from '@/lib/api/types';

type HookReturn = ReturnType<typeof hook.useMostBrewedRecipes>;

function mockHook(value: Partial<HookReturn>) {
  vi.spyOn(hook, 'useMostBrewedRecipes').mockReturnValue(value as HookReturn);
}

afterEach(() => vi.restoreAllMocks());

describe('MostBrewedRecipes', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<MostBrewedRecipes />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderWithTheme(<MostBrewedRecipes />);
    expect(screen.getByText(/could not load most-brewed recipes/i)).toBeInTheDocument();
  });

  it('shows an empty state when there are no sessions', () => {
    mockHook({ isLoading: false, isError: false, data: [] });
    renderWithTheme(<MostBrewedRecipes />);
    expect(screen.getByText(/no brew sessions yet/i)).toBeInTheDocument();
  });

  it('renders ranked rows with recipe links and session counts', () => {
    const data: MostBrewedRecipe[] = [
      { recipeId: 2, recipeName: 'Busy', totalSessions: 9 },
      { recipeId: 1, recipeName: 'Quiet', totalSessions: 3 },
    ];
    mockHook({ isLoading: false, isError: false, data });

    renderWithTheme(<MostBrewedRecipes />);

    expect(screen.getByRole('link', { name: 'Busy' })).toHaveAttribute('href', '/recipes/2');
    expect(screen.getByText('9')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Quiet' })).toHaveAttribute('href', '/recipes/1');
  });
});
