import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { LoginForm } from './LoginForm';

const pushMock = vi.fn();
vi.mock('next/navigation', () => ({ useRouter: () => ({ push: pushMock, replace: vi.fn() }) }));

const loginMock = vi.fn();
vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => ({ login: loginMock }) }));

describe('LoginForm', () => {
  afterEach(() => vi.clearAllMocks());

  it('validates required fields', async () => {
    render(<LoginForm />);
    await userEvent.click(screen.getByRole('button', { name: /log in/i }));
    expect(await screen.findByText(/email is required/i)).toBeInTheDocument();
  });

  it('submits credentials and redirects on success', async () => {
    loginMock.mockResolvedValue(undefined);
    render(<LoginForm />);
    await userEvent.type(screen.getByLabelText(/email/i), 'a@b.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'password1');
    await userEvent.click(screen.getByRole('button', { name: /log in/i }));
    await waitFor(() => expect(loginMock).toHaveBeenCalledWith({ email: 'a@b.com', password: 'password1' }));
    expect(pushMock).toHaveBeenCalledWith('/dashboard');
  });

  it('shows an error alert on 401', async () => {
    loginMock.mockRejectedValue(new Error('bad creds'));
    render(<LoginForm />);
    await userEvent.type(screen.getByLabelText(/email/i), 'a@b.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'password1');
    await userEvent.click(screen.getByRole('button', { name: /log in/i }));
    expect(await screen.findByText(/could not log in/i)).toBeInTheDocument();
  });
});
