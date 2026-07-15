import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/lib/api/client';
import { ResetPasswordForm } from './ResetPasswordForm';

const resetPasswordMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({
  resetPassword: (body: unknown) => resetPasswordMock(body),
}));

let tokenValue: string | null = 'valid-token';
vi.mock('next/navigation', () => ({
  useSearchParams: () => ({ get: () => tokenValue }),
}));

describe('ResetPasswordForm', () => {
  afterEach(() => {
    vi.clearAllMocks();
    tokenValue = 'valid-token';
  });

  it('requires a new password of at least 8 characters', async () => {
    render(<ResetPasswordForm />);
    await userEvent.type(screen.getByLabelText(/^new password$/i), 'short');
    await userEvent.type(screen.getByLabelText(/^confirm new password$/i), 'short');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));
    expect(await screen.findByText(/at least 8 characters/i)).toBeInTheDocument();
    expect(resetPasswordMock).not.toHaveBeenCalled();
  });

  it('flags mismatched confirmation', async () => {
    render(<ResetPasswordForm />);
    await userEvent.type(screen.getByLabelText(/^new password$/i), 'newpassword1');
    await userEvent.type(screen.getByLabelText(/^confirm new password$/i), 'different1');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));
    expect(await screen.findByText(/passwords do not match/i)).toBeInTheDocument();
  });

  it('submits the token and new password, then shows success', async () => {
    resetPasswordMock.mockResolvedValue(undefined);
    render(<ResetPasswordForm />);
    await userEvent.type(screen.getByLabelText(/^new password$/i), 'newpassword1');
    await userEvent.type(screen.getByLabelText(/^confirm new password$/i), 'newpassword1');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));
    await waitFor(() =>
      expect(resetPasswordMock).toHaveBeenCalledWith({
        token: 'valid-token',
        newPassword: 'newpassword1',
      }),
    );
    expect(await screen.findByText(/your password has been reset/i)).toBeInTheDocument();
  });

  it('shows invalid-link message on a 400 response', async () => {
    resetPasswordMock.mockRejectedValue(new ApiError(400, 'bad', '/api/auth/reset-password'));
    render(<ResetPasswordForm />);
    await userEvent.type(screen.getByLabelText(/^new password$/i), 'newpassword1');
    await userEvent.type(screen.getByLabelText(/^confirm new password$/i), 'newpassword1');
    await userEvent.click(screen.getByRole('button', { name: /reset password/i }));
    expect(await screen.findByText(/invalid or has expired/i)).toBeInTheDocument();
  });
});
