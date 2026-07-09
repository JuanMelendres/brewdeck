import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { FavoriteRecipesView } from './FavoriteRecipesView';
import * as favoritesHook from '@/hooks/useFavoriteRecipes';
import type { PageResponse, Recipe } from '@/lib/api/types';

vi.mock('./RecipeFormDialog', () => ({
  RecipeFormDialog: ({ open }: { open: boolean }) => (open ? <div>Edit recipe</div> : null),
}));
vi.mock('./DeleteRecipeDialog', () => ({
  DeleteRecipeDialog: ({ open }: { open: boolean }) => (open ? <div>Delete recipe</div> : null),
}));

type HookReturn = ReturnType<typeof favoritesHook.useFavoriteRecipes>;

function mockHook(value: Partial<HookReturn>) {
  return vi.spyOn(favoritesHook, 'useFavoriteRecipes').mockReturnValue(value as HookReturn);
}

function page(content: Recipe[], totalElements: number): PageResponse<Recipe> {
  return { content, page: 0, size: 10, totalElements, totalPages: 1, first: true, last: true };
}

const recipe: Recipe = {
  id: 1, coffeeId: 1, coffeeName: 'Mezcla', methodId: 1, methodName: 'AeroPress',
  name: 'Mezcla AeroPress', coffeeGrams: 15, waterGrams: 230, ratio: '1:15',
  grindSetting: null, waterTemp: 90, brewTime: null, steps: null, expectedTaste: null,
  favorite: true, createdAt: '2026-01-01T00:00:00', updatedAt: null, shareToken: null,
};

afterEach(() => vi.restoreAllMocks());

describe('FavoriteRecipesView', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<FavoriteRecipesView />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderWithTheme(<FavoriteRecipesView />);
    expect(screen.getByText(/could not load favorite recipes/i)).toBeInTheDocument();
  });

  it('shows an empty state when there are no favorites', () => {
    mockHook({ isLoading: false, isError: false, data: page([], 0) });
    renderWithTheme(<FavoriteRecipesView />);
    expect(screen.getByText(/no favorite recipes yet/i)).toBeInTheDocument();
  });

  it('renders the table on success', () => {
    mockHook({ isLoading: false, isError: false, data: page([recipe], 1) });
    renderWithTheme(<FavoriteRecipesView />);
    expect(screen.getByRole('link', { name: 'Mezcla AeroPress' })).toHaveAttribute(
      'href',
      '/recipes/1',
    );
  });

  it('opens the edit dialog when the edit action is clicked', () => {
    mockHook({ isLoading: false, isError: false, data: page([recipe], 1) });
    renderWithTheme(<FavoriteRecipesView />);

    expect(screen.queryByText('Edit recipe')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'edit' }));
    expect(screen.getByText('Edit recipe')).toBeInTheDocument();
  });
});
