import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ForgotPasswordForm } from './ForgotPasswordForm';

const forgotPasswordMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({
  forgotPassword: (body: unknown) => forgotPasswordMock(body),
}));

describe('ForgotPasswordForm', () => {
  afterEach(() => vi.clearAllMocks());

  it('validates the email field', async () => {
    render(<ForgotPasswordForm />);
    await userEvent.click(screen.getByRole('button', { name: /send reset link/i }));
    expect(await screen.findByText(/email is required/i)).toBeInTheDocument();
    expect(forgotPasswordMock).not.toHaveBeenCalled();
  });

  it('submits the email and shows the generic confirmation', async () => {
    forgotPasswordMock.mockResolvedValue({ message: 'ok' });
    render(<ForgotPasswordForm />);
    await userEvent.type(screen.getByLabelText(/email/i), 'a@b.com');
    await userEvent.click(screen.getByRole('button', { name: /send reset link/i }));
    await waitFor(() => expect(forgotPasswordMock).toHaveBeenCalledWith({ email: 'a@b.com' }));
    expect(await screen.findByText(/if that email exists/i)).toBeInTheDocument();
  });

  it('shows an error alert on failure', async () => {
    forgotPasswordMock.mockRejectedValue(new Error('network'));
    render(<ForgotPasswordForm />);
    await userEvent.type(screen.getByLabelText(/email/i), 'a@b.com');
    await userEvent.click(screen.getByRole('button', { name: /send reset link/i }));
    expect(await screen.findByText(/could not send the reset link/i)).toBeInTheDocument();
  });
});
