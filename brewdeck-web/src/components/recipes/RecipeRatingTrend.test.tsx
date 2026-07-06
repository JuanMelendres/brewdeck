import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { RecipeRatingTrend } from './RecipeRatingTrend';
import * as hook from '@/hooks/useRecipeBrewSessions';
import type { BrewSession, PageResponse } from '@/lib/api/types';

type HookReturn = ReturnType<typeof hook.useRecipeBrewSessions>;

function mockHook(value: Partial<HookReturn>) {
  vi.spyOn(hook, 'useRecipeBrewSessions').mockReturnValue(value as HookReturn);
}

function session(id: number, brewedAt: string, rating: number | null): BrewSession {
  return {
    id,
    recipeId: 1,
    recipeName: 'Recipe',
    brewedAt,
    actualGrind: null,
    actualTemp: null,
    actualTime: null,
    tasteResult: null,
    rating,
    adjustmentNotes: null,
  };
}

function page(content: BrewSession[]): PageResponse<BrewSession> {
  return { content, page: 0, size: 50, totalElements: content.length, totalPages: 1, first: true, last: true };
}

afterEach(() => vi.restoreAllMocks());

describe('RecipeRatingTrend', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<RecipeRatingTrend recipeId={1} />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderWithTheme(<RecipeRatingTrend recipeId={1} />);
    expect(screen.getByText(/could not load rating trend/i)).toBeInTheDocument();
  });

  it('shows a note when there are fewer than two rated sessions', () => {
    mockHook({
      isLoading: false,
      isError: false,
      data: page([session(1, '2026-07-01T10:00:00', 8), session(2, '2026-07-02T10:00:00', null)]),
    });
    renderWithTheme(<RecipeRatingTrend recipeId={1} />);
    expect(screen.getByText(/not enough rated sessions to show a trend/i)).toBeInTheDocument();
  });

  it('renders the chart section when there are enough rated sessions', () => {
    mockHook({
      isLoading: false,
      isError: false,
      data: page([
        session(1, '2026-07-01T10:00:00', 7),
        session(2, '2026-07-02T10:00:00', 9),
        session(3, '2026-07-03T10:00:00', 8),
      ]),
    });
    renderWithTheme(<RecipeRatingTrend recipeId={1} />);

    expect(screen.getByRole('heading', { name: 'Rating trend' })).toBeInTheDocument();
    expect(screen.queryByText(/not enough rated sessions/i)).not.toBeInTheDocument();
  });
});
