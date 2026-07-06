import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { DashboardView } from './DashboardView';
import * as hook from '@/hooks/useDashboardSummary';

vi.mock('./TopRatedRecipes', () => ({
  TopRatedRecipes: () => <div>Top Rated Recipes</div>,
}));

vi.mock('./MostBrewedRecipes', () => ({
  MostBrewedRecipes: () => <div>Most Brewed Recipes</div>,
}));

type HookReturn = ReturnType<typeof hook.useDashboardSummary>;

function mockHook(value: Partial<HookReturn>) {
  vi.spyOn(hook, 'useDashboardSummary').mockReturnValue(value as HookReturn);
}

afterEach(() => vi.restoreAllMocks());

describe('DashboardView', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<DashboardView />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, error: new Error('boom'), refetch: vi.fn() });
    renderWithTheme(<DashboardView />);
    expect(screen.getByText(/could not load/i)).toBeInTheDocument();
  });

  it('renders stat cards on success, with an em dash for a null rating', () => {
    mockHook({
      isLoading: false,
      isError: false,
      data: {
        totalCoffees: 5,
        totalBrewMethods: 4,
        totalRecipes: 10,
        favoriteRecipes: 3,
        totalBrewSessions: 20,
        averageSessionRating: null,
      },
    });
    renderWithTheme(<DashboardView />);

    expect(screen.getByText('Coffees')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('Average Rating')).toBeInTheDocument();
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
