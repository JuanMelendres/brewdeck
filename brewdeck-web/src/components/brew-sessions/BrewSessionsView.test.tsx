import { fireEvent, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewSessionsView } from './BrewSessionsView';
import * as sessionsHook from '@/hooks/useBrewSessions';
import type { BrewSession, PageResponse } from '@/lib/api/types';

vi.mock('./BrewSessionFormDialog', () => ({
  BrewSessionFormDialog: ({ open }: { open: boolean }) =>
    open ? <div>Add brew session</div> : null,
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

afterEach(() => vi.restoreAllMocks());

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

  it('shows an empty state when there are no sessions', () => {
    mockHook({ isLoading: false, isError: false, data: page([], 0) });
    renderWithTheme(<BrewSessionsView />);
    expect(screen.getByText(/no brew sessions found/i)).toBeInTheDocument();
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
});
