import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { CoffeesView } from './CoffeesView';
import * as coffeesHook from '@/hooks/useCoffees';
import type { Coffee, PageResponse } from '@/lib/api/types';

type HookReturn = ReturnType<typeof coffeesHook.useCoffees>;

function mockHook(value: Partial<HookReturn>) {
  return vi.spyOn(coffeesHook, 'useCoffees').mockReturnValue(value as HookReturn);
}

function page(content: Coffee[], totalElements: number): PageResponse<Coffee> {
  return { content, page: 0, size: 10, totalElements, totalPages: 1, first: true, last: true };
}

const coffee: Coffee = {
  id: 1, name: 'Mezcla Veracruz', brand: null, origin: null, region: null, farm: null,
  producer: null, variety: null, process: null, roastLevel: null, notesPrimary: null,
  notesSecondary: null, acidity: null, body: null, sweetness: null, bitterness: null,
  description: null, createdAt: '2026-01-01T00:00:00', updatedAt: null,
};

afterEach(() => vi.restoreAllMocks());

describe('CoffeesView', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<CoffeesView />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderWithTheme(<CoffeesView />);
    expect(screen.getByText(/could not load coffees/i)).toBeInTheDocument();
  });

  it('shows an empty state when there are no coffees', () => {
    mockHook({ isLoading: false, isError: false, data: page([], 0) });
    renderWithTheme(<CoffeesView />);
    expect(screen.getByText(/no coffees found/i)).toBeInTheDocument();
  });

  it('renders the table on success', () => {
    mockHook({ isLoading: false, isError: false, data: page([coffee], 1) });
    renderWithTheme(<CoffeesView />);
    expect(screen.getByText('Mezcla Veracruz')).toBeInTheDocument();
  });

  it('resets to page 0 when a filter changes', () => {
    const hookMock = mockHook({ isLoading: false, isError: false, data: page([coffee], 100) });
    renderWithTheme(<CoffeesView />);

    fireEvent.click(screen.getByRole('button', { name: /next page/i }));
    expect(hookMock).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }));

    fireEvent.change(screen.getByLabelText('Name'), { target: { value: 'Blend' } });
    expect(hookMock).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0 }));
  });
});
