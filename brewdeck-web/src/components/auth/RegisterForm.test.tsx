import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { RegisterForm } from './RegisterForm';
import { renderWithTheme } from '@/test/renderWithTheme';

const pushMock = vi.fn();
vi.mock('next/navigation', () => ({ useRouter: () => ({ push: pushMock, replace: vi.fn() }) }));

const registerMock = vi.fn();
vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => ({ register: registerMock }) }));

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

  it('registers with email and password only, then goes to the dashboard', async () => {
    registerMock.mockResolvedValue(undefined);
    renderWithTheme(<RegisterForm />);
    await fillForm('password1', 'password1');
    await waitFor(() =>
      expect(registerMock).toHaveBeenCalledWith({
        email: 'brewer@example.com',
        password: 'password1',
      }),
    );
    expect(pushMock).toHaveBeenCalledWith('/dashboard');
  });

  it('shows an error when registration fails', async () => {
    registerMock.mockRejectedValue(new Error('conflict'));
    renderWithTheme(<RegisterForm />);
    await fillForm('password1', 'password1');
    expect(await screen.findByText(/could not register/i)).toBeInTheDocument();
    expect(pushMock).not.toHaveBeenCalled();
  });
});
