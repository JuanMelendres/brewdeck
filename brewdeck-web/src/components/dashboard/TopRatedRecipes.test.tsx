import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { TopRatedRecipes } from './TopRatedRecipes';
import * as hook from '@/hooks/useTopRatedRecipes';
import type { TopRatedRecipe } from '@/lib/api/types';

type HookReturn = ReturnType<typeof hook.useTopRatedRecipes>;

function mockHook(value: Partial<HookReturn>) {
  vi.spyOn(hook, 'useTopRatedRecipes').mockReturnValue(value as HookReturn);
}

afterEach(() => vi.restoreAllMocks());

describe('TopRatedRecipes', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<TopRatedRecipes />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderWithTheme(<TopRatedRecipes />);
    expect(screen.getByText(/could not load top-rated recipes/i)).toBeInTheDocument();
  });

  it('shows an empty state when there are no rated recipes', () => {
    mockHook({ isLoading: false, isError: false, data: [] });
    renderWithTheme(<TopRatedRecipes />);
    expect(screen.getByText(/no rated recipes yet/i)).toBeInTheDocument();
  });

  it('renders ranked rows with recipe links and formatted ratings', () => {
    const data: TopRatedRecipe[] = [
      { recipeId: 2, recipeName: 'Best', averageRating: 9, totalSessions: 4 },
      { recipeId: 1, recipeName: 'Good', averageRating: 7.25, totalSessions: 2 },
    ];
    mockHook({ isLoading: false, isError: false, data });

    renderWithTheme(<TopRatedRecipes />);

    expect(screen.getByRole('link', { name: 'Best' })).toHaveAttribute('href', '/recipes/2');
    expect(screen.getByText('9.0')).toBeInTheDocument();
    expect(screen.getByText('7.3')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Good' })).toHaveAttribute('href', '/recipes/1');
  });
});
