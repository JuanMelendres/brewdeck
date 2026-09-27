import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { LoginForm } from './LoginForm';
import { ApiError } from '@/lib/api/client';

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

  it('tells the user how long to wait when rate limited', async () => {
    loginMock.mockRejectedValue(new ApiError(429, 'Too many attempts. Try again in 5 minutes.'));
    render(<LoginForm />);
    await userEvent.type(screen.getByLabelText(/email/i), 'a@b.com');
    await userEvent.type(screen.getByLabelText(/password/i), 'password1');
    await userEvent.click(screen.getByRole('button', { name: /log in/i }));
    expect(await screen.findByText('Too many attempts. Try again in 5 minutes.')).toBeInTheDocument();
    expect(screen.queryByText(/could not log in/i)).not.toBeInTheDocument();
  });
});
