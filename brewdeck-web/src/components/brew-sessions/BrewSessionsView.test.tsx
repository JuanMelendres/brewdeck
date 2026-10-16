import { fireEvent, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewSessionsView } from './BrewSessionsView';
import * as sessionsHook from '@/hooks/useBrewSessions';
import type { BrewSession, PageResponse } from '@/lib/api/types';

vi.mock('./BrewSessionFormDialog', () => ({
  BrewSessionFormDialog: ({ open, recipeId }: { open: boolean; recipeId?: number }) =>
    open ? <div>Add brew session{recipeId ? ` for recipe ${recipeId}` : ''}</div> : null,
}));
// Mutable so a test can start with no coffees or recipes.
const options = vi.hoisted(() => ({
  coffees: [{ id: 1, name: 'Mezcla' }],
  recipes: [{ id: 1, name: 'Mezcla AeroPress' }],
}));
vi.mock('@/hooks/useResourceOptions', () => ({
  useCoffeeOptions: () => ({ data: options.coffees, isLoading: false }),
  useRecipeOptions: () => ({ data: options.recipes, isLoading: false }),
}));
// Stand-ins for the real forms (they need a query client): saving "creates" coffee 7 / recipe 8.
vi.mock('@/components/coffees/CoffeeFormDialog', () => ({
  CoffeeFormDialog: ({ onCreated }: { onCreated?: (coffee: { id: number }) => void }) => (
    <button type="button" onClick={() => onCreated?.({ id: 7 })}>
      Save new coffee
    </button>
  ),
}));
vi.mock('@/components/recipes/RecipeFormDialog', () => ({
  RecipeFormDialog: ({
    initialCoffeeId,
    onCreated,
  }: {
    initialCoffeeId?: number;
    onCreated?: (recipe: { id: number }) => void;
  }) => (
    <button type="button" onClick={() => onCreated?.({ id: 8 })}>
      Save new recipe{initialCoffeeId ? ` with coffee ${initialCoffeeId}` : ''}
    </button>
  ),
}));

type HookReturn = ReturnType<typeof sessionsHook.useBrewSessions>;

function mockHook(value: Partial<HookReturn>) {
  return vi.spyOn(sessionsHook, 'useBrewSessions').mockReturnValue(value as HookReturn);
}

function page(content: BrewSession[], totalElements: number): PageResponse<BrewSession> {
  return { content, page: 0, size: 10, totalElements, totalPages: 1, first: true, last: true };
}

const session: BrewSession = {
  id: 1, recipeId: 1, recipeName: 'Mezcla AeroPress', brewedAt: '2026-01-01T10:30:00',
  actualGrind: null, actualTemp: 90, actualTime: '2:30', tasteResult: 'Clean',
  rating: 9, adjustmentNotes: null,
};

afterEach(() => {
  vi.restoreAllMocks();
  options.coffees = [{ id: 1, name: 'Mezcla' }];
  options.recipes = [{ id: 1, name: 'Mezcla AeroPress' }];
});

describe('BrewSessionsView', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<BrewSessionsView />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderWithTheme(<BrewSessionsView />);
    expect(screen.getByText(/could not load brew sessions/i)).toBeInTheDocument();
  });

  it('invites the user to log a first brew when there are none', () => {
    mockHook({ isLoading: false, isError: false, data: page([], 0) });
    renderWithTheme(<BrewSessionsView />);
    expect(screen.getByText(/no brew sessions yet/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /log your first brew/i })).toBeInTheDocument();
  });

  it('renders the table on success', () => {
    mockHook({ isLoading: false, isError: false, data: page([session], 1) });
    renderWithTheme(<BrewSessionsView />);
    expect(screen.getByText('Mezcla AeroPress')).toBeInTheDocument();
  });

  it('opens the create dialog when Add Brew Session is clicked', () => {
    mockHook({ isLoading: false, isError: false, data: page([session], 1) });
    renderWithTheme(<BrewSessionsView />);

    expect(screen.queryByText('Add brew session')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /add brew session/i }));
    expect(screen.getByText('Add brew session')).toBeInTheDocument();
  });

  it('resets to page 0 when the filter changes', () => {
    const hookMock = mockHook({ isLoading: false, isError: false, data: page([session], 100) });
    renderWithTheme(<BrewSessionsView />);

    fireEvent.click(screen.getByRole('button', { name: /next page/i }));
    expect(hookMock).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }));

    fireEvent.change(screen.getByLabelText('Rating'), { target: { value: '8' } });
    expect(hookMock).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0 }));
  });

  describe('without the items a brew session depends on', () => {
    it('asks for a recipe first, then opens the brew form with the new recipe', () => {
      options.recipes = [];
      mockHook({ isLoading: false, isError: false, data: page([], 0) });
      renderWithTheme(<BrewSessionsView />);

      expect(screen.getByText(/each brew session follows a recipe/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /^add brew session$/i })).toBeDisabled();

      fireEvent.click(screen.getByRole('button', { name: /add a recipe/i }));
      fireEvent.click(screen.getByRole('button', { name: /^save new recipe$/i }));

      expect(screen.getByText('Add brew session for recipe 8')).toBeInTheDocument();
    });

    it('walks coffee, then recipe, then brew session when nothing exists yet', () => {
      options.coffees = [];
      options.recipes = [];
      mockHook({ isLoading: false, isError: false, data: page([], 0) });
      renderWithTheme(<BrewSessionsView />);

      expect(screen.getByText(/recipes start from a coffee/i)).toBeInTheDocument();
      fireEvent.click(screen.getByRole('button', { name: /add a coffee/i }));
      fireEvent.click(screen.getByRole('button', { name: /save new coffee/i }));
      fireEvent.click(screen.getByRole('button', { name: /save new recipe with coffee 7/i }));

      expect(screen.getByText('Add brew session for recipe 8')).toBeInTheDocument();
    });
  });

  describe('on a phone', () => {
    // jsdom has no matchMedia; report every query as matching so the phone layout renders.
    beforeEach(() => {
      vi.stubGlobal('matchMedia', (query: string) => ({
        matches: true,
        media: query,
        onchange: null,
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        addListener: vi.fn(),
        removeListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }));
    });
    afterEach(() => vi.unstubAllGlobals());

    it('shows cards instead of the table, each linking to its session', () => {
      mockHook({ isLoading: false, isError: false, data: page([session], 1) });
      renderWithTheme(<BrewSessionsView />);

      expect(screen.queryByRole('table')).not.toBeInTheDocument();
      expect(screen.getByRole('link', { name: /open the brew session from/i })).toHaveAttribute(
        'href',
        '/brew-sessions/1',
      );
      expect(screen.getByText('Mezcla AeroPress')).toBeInTheDocument();
      expect(screen.getByText(/90°C · 2:30/)).toBeInTheDocument();
    });
  });
});
