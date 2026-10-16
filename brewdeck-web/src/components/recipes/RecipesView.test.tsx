import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { RecipesView } from './RecipesView';
import * as recipesHook from '@/hooks/useRecipes';
import type { PageResponse, Recipe } from '@/lib/api/types';

// The create dialog renders the flag-gated AI button; enable it so the dialog mounts in tests.
vi.mock('@/lib/featureFlags/FeatureFlagProvider', () => ({
  useFeatureFlag: () => true,
  useFeatureFlags: () => ({ flags: { aiRecipeAssistant: true }, status: 'ready' }),
}));
vi.mock('@/hooks/useRecipeMutations', () => ({
  useCreateRecipe: () => ({ mutate: vi.fn(), isPending: false }),
  useUpdateRecipe: () => ({ mutate: vi.fn(), isPending: false }),
  useDeleteRecipe: () => ({ mutate: vi.fn(), isPending: false }),
  useToggleFavorite: () => ({ mutate: vi.fn(), isPending: false }),
}));
// Mutable so a test can start with no coffees at all.
const options = vi.hoisted(() => ({ coffees: [{ id: 1, name: 'Mezcla' }] }));
vi.mock('@/hooks/useResourceOptions', () => ({
  useCoffeeOptions: () => ({ data: options.coffees, isLoading: false }),
  useMethodOptions: () => ({ data: [], isLoading: false }),
}));
// The real coffee form needs a query client; this stand-in "creates" coffee 7 on save.
vi.mock('@/components/coffees/CoffeeFormDialog', () => ({
  CoffeeFormDialog: ({ onCreated }: { onCreated?: (coffee: { id: number }) => void }) => (
    <button type="button" onClick={() => onCreated?.({ id: 7 })}>
      Save new coffee
    </button>
  ),
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
  favorite: true, createdAt: '2026-01-01T00:00:00', updatedAt: null, shareToken: null,
};

afterEach(() => {
  vi.restoreAllMocks();
  options.coffees = [{ id: 1, name: 'Mezcla' }];
});

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

  it('invites the user to add a first recipe when there are none', () => {
    mockHook({ isLoading: false, isError: false, data: page([], 0) });
    renderWithTheme(<RecipesView />);
    expect(screen.getByText(/no recipes yet/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /add your first recipe/i }));
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('renders the recipe cards on success', () => {
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

  describe('without any coffee yet', () => {
    it('asks for a coffee first and disables Add Recipe', () => {
      options.coffees = [];
      mockHook({ isLoading: false, isError: false, data: page([], 0) });
      renderWithTheme(<RecipesView />);

      expect(screen.getByText(/recipes start from a coffee/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /^add recipe$/i })).toBeDisabled();
      expect(screen.queryByRole('button', { name: /add your first recipe/i })).not.toBeInTheDocument();
    });

    it('opens the recipe form with the new coffee selected once the coffee is saved', () => {
      options.coffees = [];
      mockHook({ isLoading: false, isError: false, data: page([], 0) });
      const { rerender } = renderWithTheme(<RecipesView />);

      fireEvent.click(screen.getByRole('button', { name: /add a coffee/i }));
      // The saved coffee shows up in the options once the coffee list refetches.
      options.coffees = [{ id: 7, name: 'Nuevo' }];
      fireEvent.click(screen.getByRole('button', { name: /save new coffee/i }));
      rerender(<RecipesView />);

      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByLabelText(/coffee/i, { selector: 'select' })).toHaveValue('7');
    });
  });
});
