import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/lib/api/client';
import { ChangePasswordForm } from './ChangePasswordForm';

const changePasswordMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({
  changePassword: (body: unknown) => changePasswordMock(body),
}));

async function fill(current: string, next: string, confirm: string) {
  await userEvent.type(screen.getByLabelText(/^current password$/i), current);
  await userEvent.type(screen.getByLabelText(/^new password$/i), next);
  await userEvent.type(screen.getByLabelText(/^confirm new password$/i), confirm);
}

describe('ChangePasswordForm', () => {
  afterEach(() => vi.clearAllMocks());

  it('requires a new password of at least 8 characters', async () => {
    render(<ChangePasswordForm />);
    await fill('password1', 'short', 'short');
    await userEvent.click(screen.getByRole('button', { name: /change password/i }));
    expect(await screen.findByText(/at least 8 characters/i)).toBeInTheDocument();
    expect(changePasswordMock).not.toHaveBeenCalled();
  });

  it('flags mismatched confirmation', async () => {
    render(<ChangePasswordForm />);
    await fill('password1', 'newpassword1', 'different1');
    await userEvent.click(screen.getByRole('button', { name: /change password/i }));
    expect(await screen.findByText(/passwords do not match/i)).toBeInTheDocument();
    expect(changePasswordMock).not.toHaveBeenCalled();
  });

  it('submits the current and new password and shows success', async () => {
    changePasswordMock.mockResolvedValue(undefined);
    render(<ChangePasswordForm />);
    await fill('password1', 'newpassword1', 'newpassword1');
    await userEvent.click(screen.getByRole('button', { name: /change password/i }));
    await waitFor(() =>
      expect(changePasswordMock).toHaveBeenCalledWith({
        currentPassword: 'password1',
        newPassword: 'newpassword1',
      }),
    );
    expect(await screen.findByText(/password changed/i)).toBeInTheDocument();
  });

  it('flags a wrong current password from a 400 response', async () => {
    changePasswordMock.mockRejectedValue(
      new ApiError(400, 'Current password is incorrect', '/api/auth/change-password'),
    );
    render(<ChangePasswordForm />);
    await fill('wrong', 'newpassword1', 'newpassword1');
    await userEvent.click(screen.getByRole('button', { name: /change password/i }));
    expect(await screen.findByText(/current password is incorrect/i)).toBeInTheDocument();
  });
});
