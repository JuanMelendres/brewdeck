import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { MostUsedCoffees } from './MostUsedCoffees';
import * as hook from '@/hooks/useMostUsedCoffees';
import type { MostUsedCoffee } from '@/lib/api/coffees';

type HookReturn = ReturnType<typeof hook.useMostUsedCoffees>;

function mockHook(value: Partial<HookReturn>) {
  vi.spyOn(hook, 'useMostUsedCoffees').mockReturnValue(value as HookReturn);
}

afterEach(() => vi.restoreAllMocks());

describe('MostUsedCoffees', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<MostUsedCoffees />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderWithTheme(<MostUsedCoffees />);
    expect(screen.getByText(/could not load most-used coffees/i)).toBeInTheDocument();
  });

  it('shows an empty state when no coffees are used', () => {
    mockHook({ isLoading: false, isError: false, data: [] });
    renderWithTheme(<MostUsedCoffees />);
    expect(screen.getByText(/no coffees used in recipes yet/i)).toBeInTheDocument();
  });

  it('renders ranked rows with recipe counts', () => {
    const data: MostUsedCoffee[] = [
      { coffeeId: 2, coffeeName: 'Popular', recipeCount: 7 },
      { coffeeId: 1, coffeeName: 'Rare', recipeCount: 1 },
    ];
    mockHook({ isLoading: false, isError: false, data });

    renderWithTheme(<MostUsedCoffees />);

    expect(screen.getByText('Popular')).toBeInTheDocument();
    expect(screen.getByText('7')).toBeInTheDocument();
    expect(screen.getByText('Rare')).toBeInTheDocument();
  });
});
