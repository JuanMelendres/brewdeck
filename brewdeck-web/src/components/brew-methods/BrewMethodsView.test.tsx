import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewMethodsView } from './BrewMethodsView';
import * as methodsHook from '@/hooks/useBrewMethods';
import type { BrewMethod } from '@/lib/api/brewMethods';
import type { PageResponse } from '@/lib/api/types';

type HookReturn = ReturnType<typeof methodsHook.useBrewMethods>;

function mockHook(value: Partial<HookReturn>) {
  return vi.spyOn(methodsHook, 'useBrewMethods').mockReturnValue(value as HookReturn);
}

function page(content: BrewMethod[], totalElements: number): PageResponse<BrewMethod> {
  return { content, page: 0, size: 10, totalElements, totalPages: 1, first: true, last: true };
}

const method: BrewMethod = {
  id: 1, name: 'AeroPress', description: 'Immersion', createdAt: '2026-01-01T00:00:00', updatedAt: null,
};

afterEach(() => vi.restoreAllMocks());

describe('BrewMethodsView', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderWithTheme(<BrewMethodsView />);
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state on failure', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderWithTheme(<BrewMethodsView />);
    expect(screen.getByText(/could not load brew methods/i)).toBeInTheDocument();
  });

  it('shows an empty state when there are no methods', () => {
    mockHook({ isLoading: false, isError: false, data: page([], 0) });
    renderWithTheme(<BrewMethodsView />);
    expect(screen.getByText(/no brew methods found/i)).toBeInTheDocument();
  });

  it('renders the table on success', () => {
    mockHook({ isLoading: false, isError: false, data: page([method], 1) });
    renderWithTheme(<BrewMethodsView />);
    expect(screen.getByText('AeroPress')).toBeInTheDocument();
  });

  it('requests the next page when paginating', () => {
    const hookMock = mockHook({ isLoading: false, isError: false, data: page([method], 100) });
    renderWithTheme(<BrewMethodsView />);

    fireEvent.click(screen.getByRole('button', { name: /next page/i }));
    expect(hookMock).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }));
  });
});
