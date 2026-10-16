import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { BrewSessionDetailView } from './BrewSessionDetailView';
import * as hook from '@/hooks/useBrewSession';
import * as mutations from '@/hooks/useBrewSessionMutations';
import type { BrewSession } from '@/lib/api/types';
import { NotificationProvider } from '@/lib/notifications/NotificationProvider';

const pushMock = vi.fn();
vi.mock('next/navigation', () => ({ useRouter: () => ({ push: pushMock, replace: vi.fn() }) }));

// The edit form has its own tests; here it only has to open with the session.
vi.mock('./BrewSessionFormDialog', () => ({
  BrewSessionFormDialog: ({ session }: { session?: BrewSession }) => (
    <div role="dialog">Editing session {session?.id}</div>
  ),
}));

type HookReturn = ReturnType<typeof hook.useBrewSession>;

function mockHook(value: Partial<HookReturn>) {
  vi.spyOn(hook, 'useBrewSession').mockReturnValue(value as HookReturn);
}

const deleteMutate = vi.fn();

const session: BrewSession = {
  id: 5,
  recipeId: 3,
  recipeName: 'Mezcla AeroPress',
  brewedAt: '2026-01-01T10:30:00Z',
  actualGrind: 'Medium-fine',
  actualTemp: 92,
  actualTime: '2:30',
  tasteResult: 'Clean and sweet',
  rating: 8,
  adjustmentNotes: 'Grind a little finer next time',
};

function renderView() {
  return renderWithTheme(
    <NotificationProvider>
      <BrewSessionDetailView sessionId={5} />
    </NotificationProvider>,
  );
}

beforeEach(() => {
  pushMock.mockReset();
  deleteMutate.mockReset();
  vi.spyOn(mutations, 'useDeleteBrewSession').mockReturnValue({
    mutate: deleteMutate,
    isPending: false,
  } as never);
});
afterEach(() => vi.restoreAllMocks());

describe('BrewSessionDetailView', () => {
  it('shows a spinner while loading', () => {
    mockHook({ isLoading: true, isError: false, data: undefined });
    renderView();
    expect(screen.getByRole('status', { name: /loading/i })).toBeInTheDocument();
  });

  it('shows an error state when the session cannot be loaded', () => {
    mockHook({ isLoading: false, isError: true, data: undefined, refetch: vi.fn() });
    renderView();
    expect(screen.getByText(/could not load this brew session/i)).toBeInTheDocument();
  });

  it('shows every field of the session', () => {
    mockHook({ isLoading: false, isError: false, data: session });
    renderView();

    expect(screen.getByRole('link', { name: 'Mezcla AeroPress' })).toHaveAttribute('href', '/recipes/3');
    expect(screen.getByText('8/10')).toBeInTheDocument();
    expect(screen.getByText('Medium-fine')).toBeInTheDocument();
    expect(screen.getByText('92°C')).toBeInTheDocument();
    expect(screen.getByText('2:30')).toBeInTheDocument();
    expect(screen.getByText('Clean and sweet')).toBeInTheDocument();
    expect(screen.getByText('Grind a little finer next time')).toBeInTheDocument();
  });

  it('says "Not rated" and hides empty notes', () => {
    mockHook({
      isLoading: false,
      isError: false,
      data: { ...session, rating: null, tasteResult: null, adjustmentNotes: '' },
    });
    renderView();

    expect(screen.getByText('Not rated')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /taste result/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /adjustment notes/i })).not.toBeInTheDocument();
  });

  it('opens the edit form with this session', () => {
    mockHook({ isLoading: false, isError: false, data: session });
    renderView();

    fireEvent.click(screen.getByRole('button', { name: /edit/i }));

    expect(screen.getByRole('dialog')).toHaveTextContent('Editing session 5');
  });

  it('asks for confirmation, deletes, and returns to the list', async () => {
    deleteMutate.mockImplementation((_id, opts) => opts.onSuccess());
    mockHook({ isLoading: false, isError: false, data: session });
    renderView();

    fireEvent.click(screen.getByRole('button', { name: /^delete$/i }));
    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveTextContent(/delete the mezcla aeropress brew/i);
    expect(deleteMutate).not.toHaveBeenCalled();

    fireEvent.click(within(dialog).getByRole('button', { name: /^delete$/i }));

    expect(deleteMutate).toHaveBeenCalledWith(5, expect.anything());
    await waitFor(() => expect(pushMock).toHaveBeenCalledWith('/brew-sessions'));
  });

  it('stays on the page when the delete is cancelled', () => {
    mockHook({ isLoading: false, isError: false, data: session });
    renderView();

    fireEvent.click(screen.getByRole('button', { name: /^delete$/i }));
    fireEvent.click(screen.getByRole('button', { name: /cancel/i }));

    expect(deleteMutate).not.toHaveBeenCalled();
    expect(pushMock).not.toHaveBeenCalled();
  });
});
