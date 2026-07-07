import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { RecipesView } from './RecipesView';
import * as recipesHook from '@/hooks/useRecipes';
import type { PageResponse, Recipe } from '@/lib/api/types';

vi.mock('@/hooks/useRecipeMutations', () => ({
  useCreateRecipe: () => ({ mutate: vi.fn(), isPending: false }),
  useUpdateRecipe: () => ({ mutate: vi.fn(), isPending: false }),
  useDeleteRecipe: () => ({ mutate: vi.fn(), isPending: false }),
}));
vi.mock('@/hooks/useResourceOptions', () => ({
  useCoffeeOptions: () => ({ data: [], isLoading: false }),
  useMethodOptions: () => ({ data: [], isLoading: false }),
}));
vi.mock('@/hooks/useSuggestRecipe', () => ({
  useSuggestRecipe: () => ({ mutate: vi.fn(), isPending: false }),
}));

type HookReturn = ReturnType<typeof recipesHook.useRecipes>;

function mockHook(value: Partial<HookReturn>) {
  return vi.spyOn(recipesHook, 'useRecipes').mockReturnValue(value as HookReturn);
}

function page(content: Recipe[], totalElements: number): PageResponse<Recipe> {
  return { content, page: 0, size: 10, totalElements, totalPages: 1, first: true, last: true };
}

const recipe: Recipe = {
  id: 1, coffeeId: 1, coffeeName: 'Mezcla', methodId: 1, methodName: 'AeroPress',
  name: 'Mezcla AeroPress', coffeeGrams: 15, waterGrams: 230, ratio: '1:15',
  grindSetting: null, waterTemp: 90, brewTime: null, steps: null, expectedTaste: null,
  favorite: true, createdAt: '2026-01-01T00:00:00', updatedAt: null,
};

afterEach(() => vi.restoreAllMocks());

describe('RecipesView', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<RecipesView />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderWithTheme(<RecipesView />);
    expect(screen.getByText(/could not load recipes/i)).toBeInTheDocument();
  });

  it('shows an empty state when there are no recipes', () => {
    mockHook({ isLoading: false, isError: false, data: page([], 0) });
    renderWithTheme(<RecipesView />);
    expect(screen.getByText(/no recipes found/i)).toBeInTheDocument();
  });

  it('renders the table on success', () => {
    mockHook({ isLoading: false, isError: false, data: page([recipe], 1) });
    renderWithTheme(<RecipesView />);
    expect(screen.getByText('Mezcla AeroPress')).toBeInTheDocument();
  });

  it('resets to page 0 when a filter changes', () => {
    const hookMock = mockHook({ isLoading: false, isError: false, data: page([recipe], 100) });
    renderWithTheme(<RecipesView />);

    fireEvent.click(screen.getByRole('button', { name: /next page/i }));
    expect(hookMock).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }));

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'V60' } });
    expect(hookMock).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0 }));
  });

  it('opens the create dialog when Add Recipe is clicked', () => {
    mockHook({ isLoading: false, isError: false, data: page([], 0) });
    renderWithTheme(<RecipesView />);

    fireEvent.click(screen.getByRole('button', { name: /add recipe/i }));

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Add recipe')).toBeInTheDocument();
  });
});
