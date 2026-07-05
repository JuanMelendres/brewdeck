import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { RecipeDetailView } from './RecipeDetailView';
import * as recipeHooks from '@/hooks/useRecipe';
import type { Recipe, RecipeStats } from '@/lib/api/types';

type RecipeReturn = ReturnType<typeof recipeHooks.useRecipe>;
type StatsReturn = ReturnType<typeof recipeHooks.useRecipeStats>;

const recipe: Recipe = {
  id: 1,
  coffeeId: 1,
  coffeeName: 'Mezcla Veracruz',
  methodId: 1,
  methodName: 'AeroPress',
  name: 'Mezcla Veracruz AeroPress',
  coffeeGrams: 15,
  waterGrams: 230,
  ratio: '1:15',
  grindSetting: 'Timemore S3 - 5.5',
  waterTemp: 90,
  brewTime: '2:30',
  steps: 'Bloom 30s, stir gently, press slowly.',
  expectedTaste: 'Clean, aromatic, spicy, balanced.',
  favorite: true,
  createdAt: '2026-01-01T00:00:00',
  updatedAt: null,
};

function mockRecipe(value: Partial<RecipeReturn>) {
  vi.spyOn(recipeHooks, 'useRecipe').mockReturnValue(value as RecipeReturn);
}

function mockStats(value: Partial<StatsReturn>) {
  vi.spyOn(recipeHooks, 'useRecipeStats').mockReturnValue(value as StatsReturn);
}

afterEach(() => vi.restoreAllMocks());

describe('RecipeDetailView', () => {
  it('shows a spinner while the recipe is loading', () => {
    mockRecipe({ isLoading: true, isError: false, data: undefined });
    mockStats({ isLoading: true, isError: false, data: undefined });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state when the recipe fails to load', () => {
    mockRecipe({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    mockStats({ isLoading: false, isError: false, data: undefined });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByText(/could not load recipe\./i)).toBeInTheDocument();
  });

  it('renders recipe details and stat cards on success', () => {
    const stats: RecipeStats = {
      recipeId: 1,
      totalSessions: 3,
      averageRating: 8.5,
      lastBrewedAt: '2026-07-05T10:00:00',
    };
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: false, data: stats });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByRole('heading', { name: 'Mezcla Veracruz AeroPress' })).toBeInTheDocument();
    expect(screen.getByText('Favorite')).toBeInTheDocument();
    expect(screen.getByText('Mezcla Veracruz')).toBeInTheDocument();
    expect(screen.getByText('Total Sessions')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('8.5')).toBeInTheDocument();
  });

  it('renders an em dash for a null average rating', () => {
    const stats: RecipeStats = {
      recipeId: 1,
      totalSessions: 0,
      averageRating: null,
      lastBrewedAt: null,
    };
    mockRecipe({ isLoading: false, isError: false, data: { ...recipe, favorite: false } });
    mockStats({ isLoading: false, isError: false, data: stats });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByText('Average Rating')).toBeInTheDocument();
    expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(1);
    expect(screen.queryByText('Favorite')).not.toBeInTheDocument();
  });

  it('shows an inline stats error when statistics fail but the recipe loaded', () => {
    mockRecipe({ isLoading: false, isError: false, data: recipe });
    mockStats({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });

    renderWithTheme(<RecipeDetailView recipeId={1} />);

    expect(screen.getByRole('heading', { name: 'Mezcla Veracruz AeroPress' })).toBeInTheDocument();
    expect(screen.getByText(/could not load recipe statistics\./i)).toBeInTheDocument();
  });
});
