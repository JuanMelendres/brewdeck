import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { RegisterForm } from './RegisterForm';
import { renderWithTheme } from '@/test/renderWithTheme';

const registerMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({ register: (body: unknown) => registerMock(body) }));

async function fillForm(password: string, confirmPassword: string) {
  await userEvent.type(screen.getByLabelText(/^email$/i), 'brewer@example.com');
  await userEvent.type(screen.getByLabelText(/^password$/i), password);
  await userEvent.type(screen.getByLabelText(/^confirm password$/i), confirmPassword);
  await userEvent.click(screen.getByRole('button', { name: /create account/i }));
}

describe('RegisterForm', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('requires the password confirmation', async () => {
    renderWithTheme(<RegisterForm />);
    await userEvent.type(screen.getByLabelText(/^email$/i), 'brewer@example.com');
    await userEvent.type(screen.getByLabelText(/^password$/i), 'password1');
    await userEvent.click(screen.getByRole('button', { name: /create account/i }));
    expect(await screen.findByText(/confirm your new password/i)).toBeInTheDocument();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('flags a confirmation that does not match', async () => {
    renderWithTheme(<RegisterForm />);
    await fillForm('password1', 'password2');
    expect(await screen.findByText(/passwords do not match/i)).toBeInTheDocument();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('registers with email and password only, then asks the user to check their email', async () => {
    registerMock.mockResolvedValue({ message: 'Check your email to finish creating your account.' });
    renderWithTheme(<RegisterForm />);
    await fillForm('password1', 'password1');
    await waitFor(() =>
      expect(registerMock).toHaveBeenCalledWith({
        email: 'brewer@example.com',
        password: 'password1',
      }),
    );
    expect(await screen.findByRole('heading', { name: /check your email/i })).toBeInTheDocument();
    expect(screen.getByText(/we sent a message to brewer@example\.com/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /go to log in/i })).toHaveAttribute('href', '/login');
    // No session is created at signup, so the form is gone rather than redirecting into the app.
    expect(screen.queryByRole('button', { name: /create account/i })).not.toBeInTheDocument();
  });

  it('shows an error when registration fails', async () => {
    registerMock.mockRejectedValue(new Error('conflict'));
    renderWithTheme(<RegisterForm />);
    await fillForm('password1', 'password1');
    expect(await screen.findByText(/could not create your account/i)).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /check your email/i })).not.toBeInTheDocument();
  });
});
