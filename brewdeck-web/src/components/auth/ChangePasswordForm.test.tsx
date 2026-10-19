import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/lib/api/client';
import { ChangePasswordForm } from './ChangePasswordForm';
import { renderWithTheme } from '@/test/renderWithTheme';

const changePasswordMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({
  changePassword: (body: unknown) => changePasswordMock(body),
}));

const setTokenMock = vi.fn();
vi.mock('@/lib/auth/tokenStore', () => ({
  setToken: (token: string) => setTokenMock(token),
}));

async function fill(current: string, next: string, confirm: string) {
  await userEvent.type(screen.getByLabelText(/^current password$/i, { selector: 'input' }), current);
  await userEvent.type(screen.getByLabelText(/^new password$/i, { selector: 'input' }), next);
  await userEvent.type(screen.getByLabelText(/^confirm new password$/i, { selector: 'input' }), confirm);
}

describe('ChangePasswordForm', () => {
  afterEach(() => vi.clearAllMocks());

  it('requires a new password of at least 15 characters', async () => {
    renderWithTheme(<ChangePasswordForm />);
    await fill('brew-test-pass-001', 'short', 'short');
    await userEvent.click(screen.getByRole('button', { name: /change password/i }));
    expect(await screen.findByText(/at least 15 characters/i)).toBeInTheDocument();
    expect(changePasswordMock).not.toHaveBeenCalled();
  });

  it('flags mismatched confirmation', async () => {
    renderWithTheme(<ChangePasswordForm />);
    await fill('brew-test-pass-001', 'brew-new-pass-0001', 'brew-different-0001');
    await userEvent.click(screen.getByRole('button', { name: /change password/i }));
    expect(await screen.findByText(/passwords do not match/i)).toBeInTheDocument();
    expect(changePasswordMock).not.toHaveBeenCalled();
  });

  it('submits the current and new password, keeps the new session, and shows success', async () => {
    changePasswordMock.mockResolvedValue({
      token: 'fresh-jwt',
      expiresAt: '2026-10-16T12:00:00Z',
      email: 'brewer@example.com',
    });
    renderWithTheme(<ChangePasswordForm />);
    await fill('brew-test-pass-001', 'brew-new-pass-0001', 'brew-new-pass-0001');
    await userEvent.click(screen.getByRole('button', { name: /change password/i }));
    await waitFor(() =>
      expect(changePasswordMock).toHaveBeenCalledWith({
        currentPassword: 'brew-test-pass-001',
        newPassword: 'brew-new-pass-0001',
      }),
    );
    expect(await screen.findByText(/password changed/i)).toBeInTheDocument();
    expect(setTokenMock).toHaveBeenCalledWith('fresh-jwt');
  });

  it('flags a wrong current password from a 400 response', async () => {
    changePasswordMock.mockRejectedValue(
      new ApiError(400, 'Current password is incorrect', '/api/auth/change-password'),
    );
    renderWithTheme(<ChangePasswordForm />);
    await fill('wrong', 'brew-new-pass-0001', 'brew-new-pass-0001');
    await userEvent.click(screen.getByRole('button', { name: /change password/i }));
    expect(await screen.findByText(/current password is incorrect/i)).toBeInTheDocument();
  });
});
