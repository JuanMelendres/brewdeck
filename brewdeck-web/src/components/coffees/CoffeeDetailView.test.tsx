import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { CoffeeDetailView } from './CoffeeDetailView';
import * as hook from '@/hooks/useCoffee';
import type { Coffee } from '@/lib/api/types';

type HookReturn = ReturnType<typeof hook.useCoffee>;

function mockHook(value: Partial<HookReturn>) {
  vi.spyOn(hook, 'useCoffee').mockReturnValue(value as HookReturn);
}

const coffee: Coffee = {
  id: 1,
  name: 'Mezcla Veracruz',
  brand: 'Café local',
  origin: 'Veracruz',
  region: 'Coatepec',
  farm: null,
  producer: null,
  variety: 'Blend',
  process: 'Lavado',
  roastLevel: 'Medio',
  notesPrimary: 'Cardamomo',
  notesSecondary: 'Canela',
  acidityScore: 3,
  bodyScore: 3,
  sweetnessScore: 3,
  bitternessScore: 2,
  description: 'Clean and aromatic.',
  createdAt: '2026-01-01T00:00:00',
  updatedAt: null,
};

afterEach(() => vi.restoreAllMocks());

describe('CoffeeDetailView', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<CoffeeDetailView coffeeId={1} />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderWithTheme(<CoffeeDetailView coffeeId={1} />);
    expect(screen.getByText(/could not load coffee/i)).toBeInTheDocument();
  });

  it('renders the coffee name, attributes, notes and description', () => {
    mockHook({ isLoading: false, isError: false, data: coffee });
    renderWithTheme(<CoffeeDetailView coffeeId={1} />);

    expect(screen.getByRole('heading', { name: 'Mezcla Veracruz' })).toBeInTheDocument();
    expect(screen.getByText('Veracruz')).toBeInTheDocument();
    expect(screen.getByText('Lavado')).toBeInTheDocument();
    expect(screen.getByText('Cardamomo · Canela')).toBeInTheDocument();
    expect(screen.getByText('Clean and aromatic.')).toBeInTheDocument();
  });

  it('renders the tasting profile radar', () => {
    mockHook({ isLoading: false, isError: false, data: coffee });
    renderWithTheme(<CoffeeDetailView coffeeId={1} />);
    expect(screen.getByRole('heading', { name: 'Tasting profile' })).toBeInTheDocument();
  });
});
