import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/lib/api/client';
import { ChangeEmailForm } from './ChangeEmailForm';
import { renderWithTheme } from '@/test/renderWithTheme';

const requestEmailChangeMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({
  requestEmailChange: (body: unknown) => requestEmailChangeMock(body),
}));

vi.mock('@/lib/auth/AuthProvider', () => ({
  useAuth: () => ({ user: { email: 'brewer@example.com' } }),
}));

async function fill(newEmail: string, password: string) {
  await userEvent.type(screen.getByLabelText(/^new email$/i), newEmail);
  await userEvent.type(
    screen.getByLabelText(/^current password$/i, { selector: 'input' }),
    password,
  );
  await userEvent.click(screen.getByRole('button', { name: /send confirmation link/i }));
}

describe('ChangeEmailForm', () => {
  afterEach(() => vi.clearAllMocks());

  it('shows the current email', () => {
    renderWithTheme(<ChangeEmailForm />);
    expect(screen.getByText('Current email: brewer@example.com')).toBeInTheDocument();
  });

  it('validates the new email before calling the API', async () => {
    renderWithTheme(<ChangeEmailForm />);
    await fill('not-an-email', 'brew-test-pass-001');
    expect(await screen.findByText(/valid email/i)).toBeInTheDocument();
    expect(requestEmailChangeMock).not.toHaveBeenCalled();
  });

  it('sends the request and says where the link went without revealing more', async () => {
    requestEmailChangeMock.mockResolvedValue({ message: 'ok' });
    renderWithTheme(<ChangeEmailForm />);
    await fill('New@Example.com', 'brew-test-pass-001');
    await waitFor(() =>
      expect(requestEmailChangeMock).toHaveBeenCalledWith({
        newEmail: 'New@Example.com',
        currentPassword: 'brew-test-pass-001',
      }),
    );
    expect(
      await screen.findByText(/if new@example.com can be used, we sent it a confirmation link/i),
    ).toBeInTheDocument();
  });

  it('marks a wrong current password on its field', async () => {
    requestEmailChangeMock.mockRejectedValue(new ApiError(400, 'Current password is incorrect'));
    renderWithTheme(<ChangeEmailForm />);
    await fill('new@example.com', 'wrong-password-123');
    expect(await screen.findByText(/current password is incorrect/i)).toBeInTheDocument();
  });

  it('maps server field errors onto the fields', async () => {
    requestEmailChangeMock.mockRejectedValue(
      new ApiError(400, 'Validation failed', undefined, { newEmail: 'This is already your email' }),
    );
    renderWithTheme(<ChangeEmailForm />);
    await fill('brewer@example.com', 'brew-test-pass-001');
    expect(await screen.findByText('This is already your email')).toBeInTheDocument();
  });

  it('shows the rate-limit message from the API', async () => {
    requestEmailChangeMock.mockRejectedValue(
      new ApiError(429, 'Too many attempts. Try again in 30 minutes.'),
    );
    renderWithTheme(<ChangeEmailForm />);
    await fill('new@example.com', 'brew-test-pass-001');
    expect(await screen.findByRole('alert')).toHaveTextContent(/try again in 30 minutes/i);
  });
});
