import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { EmailChangeLinkView } from './EmailChangeLinkView';
import { renderWithTheme } from '@/test/renderWithTheme';

const confirmMock = vi.fn();
const cancelMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({
  confirmEmailChange: (t: string) => confirmMock(t),
  cancelEmailChange: (t: string) => cancelMock(t),
}));

const logoutMock = vi.fn().mockResolvedValue(undefined);
let authStatus = 'anonymous';
vi.mock('@/lib/auth/AuthProvider', () => ({
  useAuth: () => ({ status: authStatus, logout: logoutMock }),
}));

let tokenValue: string | null = 'link-token';
vi.mock('next/navigation', () => ({ useSearchParams: () => ({ get: () => tokenValue }) }));

describe('EmailChangeLinkView', () => {
  afterEach(() => {
    vi.clearAllMocks();
    tokenValue = 'link-token';
    authStatus = 'anonymous';
  });

  it('waits for a click before confirming', async () => {
    confirmMock.mockResolvedValue(undefined);
    renderWithTheme(<EmailChangeLinkView mode="confirm" />);
    expect(confirmMock).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: /confirm new email/i }));

    await waitFor(() => expect(confirmMock).toHaveBeenCalledWith('link-token'));
    expect(await screen.findByText(/your email has been changed/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /go to log in/i })).toHaveAttribute('href', '/login');
    expect(logoutMock).not.toHaveBeenCalled();
  });

  it('signs out a signed-in browser after confirming', async () => {
    confirmMock.mockResolvedValue(undefined);
    authStatus = 'authenticated';
    renderWithTheme(<EmailChangeLinkView mode="confirm" />);
    await userEvent.click(screen.getByRole('button', { name: /confirm new email/i }));
    await waitFor(() => expect(logoutMock).toHaveBeenCalled());
  });

  it('cancels and points to the password reset', async () => {
    cancelMock.mockResolvedValue(undefined);
    renderWithTheme(<EmailChangeLinkView mode="cancel" />);

    await userEvent.click(screen.getByRole('button', { name: /cancel the change/i }));

    await waitFor(() => expect(cancelMock).toHaveBeenCalledWith('link-token'));
    expect(await screen.findByText(/the change was cancelled/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /reset password/i })).toHaveAttribute(
      'href',
      '/forgot-password',
    );
  });

  it('shows the invalid-link message when the API refuses the token', async () => {
    confirmMock.mockRejectedValue(new Error('bad'));
    renderWithTheme(<EmailChangeLinkView mode="confirm" />);
    await userEvent.click(screen.getByRole('button', { name: /confirm new email/i }));
    expect(await screen.findByText(/invalid, has expired or was already used/i)).toBeInTheDocument();
  });

  it('shows the invalid-link message without a token and offers no action', () => {
    tokenValue = null;
    renderWithTheme(<EmailChangeLinkView mode="cancel" />);
    expect(screen.getByText(/invalid, has expired or was already used/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /cancel the change/i })).not.toBeInTheDocument();
  });
});
