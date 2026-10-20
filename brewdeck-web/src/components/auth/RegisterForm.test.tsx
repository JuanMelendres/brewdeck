import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { RegisterForm } from './RegisterForm';
import { renderWithTheme } from '@/test/renderWithTheme';
import { ApiError } from '@/lib/api/client';

const registerMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({ register: (body: unknown) => registerMock(body) }));

let legalStatus = { isLoading: false, published: false, termsVersion: null as string | null };
vi.mock('@/hooks/useLegalStatus', () => ({ useLegalStatus: () => legalStatus }));

async function fillForm(password: string, confirmPassword: string) {
  await userEvent.type(screen.getByLabelText(/^email$/i), 'brewer@example.com');
  await userEvent.type(screen.getByLabelText(/^password$/i, { selector: 'input' }), password);
  await userEvent.type(screen.getByLabelText(/^confirm password$/i, { selector: 'input' }), confirmPassword);
  await userEvent.click(screen.getByRole('button', { name: /create account/i }));
}

describe('RegisterForm', () => {
  afterEach(() => {
    vi.clearAllMocks();
    legalStatus = { isLoading: false, published: false, termsVersion: null };
  });

  it('asks for no consent while the legal texts are not published', async () => {
    registerMock.mockResolvedValue({ message: 'ok' });
    renderWithTheme(<RegisterForm />);
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
    await fillForm('brew-test-pass-001', 'brew-test-pass-001');
    await waitFor(() =>
      expect(registerMock).toHaveBeenCalledWith({
        email: 'brewer@example.com',
        password: 'brew-test-pass-001',
        acceptedTermsVersion: undefined,
      }),
    );
  });

  it('requires accepting the published terms and sends their version', async () => {
    legalStatus = { isLoading: false, published: true, termsVersion: '2026-10-20' };
    registerMock.mockResolvedValue({ message: 'ok' });
    renderWithTheme(<RegisterForm />);

    for (const link of screen.getAllByRole('link', { name: /privacy notice/i })) {
      expect(link).toHaveAttribute('href', '/privacy');
    }
    expect(screen.getByRole('link', { name: 'terms' })).toHaveAttribute('href', '/terms');
    await fillForm('brew-test-pass-001', 'brew-test-pass-001');
    expect(await screen.findByText(/accept the terms and the privacy notice/i)).toBeInTheDocument();
    expect(registerMock).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('checkbox', { name: /i accept the terms/i }));
    await userEvent.click(screen.getByRole('button', { name: /create account/i }));
    await waitFor(() =>
      expect(registerMock).toHaveBeenCalledWith({
        email: 'brewer@example.com',
        password: 'brew-test-pass-001',
        acceptedTermsVersion: '2026-10-20',
      }),
    );
  });

  it('requires the password confirmation', async () => {
    renderWithTheme(<RegisterForm />);
    await userEvent.type(screen.getByLabelText(/^email$/i), 'brewer@example.com');
    await userEvent.type(screen.getByLabelText(/^password$/i, { selector: 'input' }), 'brew-test-pass-001');
    await userEvent.click(screen.getByRole('button', { name: /create account/i }));
    expect(await screen.findByText(/confirm your new password/i)).toBeInTheDocument();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('flags a confirmation that does not match', async () => {
    renderWithTheme(<RegisterForm />);
    await fillForm('brew-test-pass-001', 'brew-test-pass-002');
    expect(await screen.findByText(/passwords do not match/i)).toBeInTheDocument();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('registers with email and password only, then asks the user to check their email', async () => {
    registerMock.mockResolvedValue({ message: 'Check your email to finish creating your account.' });
    renderWithTheme(<RegisterForm />);
    await fillForm('brew-test-pass-001', 'brew-test-pass-001');
    await waitFor(() =>
      expect(registerMock).toHaveBeenCalledWith({
        email: 'brewer@example.com',
        password: 'brew-test-pass-001',
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
    await fillForm('brew-test-pass-001', 'brew-test-pass-001');
    expect(await screen.findByText(/could not create your account/i)).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /check your email/i })).not.toBeInTheDocument();
  });

  it('shows the server common-password error on the password field', async () => {
    registerMock.mockRejectedValueOnce(
      new ApiError(400, 'Validation failed', '/api/auth/register', {
        password: 'This password is too common. Choose a different one',
      }),
    );
    renderWithTheme(<RegisterForm />);
    await fillForm('passwordpassword', 'passwordpassword');

    expect(
      await screen.findByText('This password is too common. Choose a different one'),
    ).toBeInTheDocument();
    expect(screen.getByLabelText(/^password$/i, { selector: 'input' })).toHaveAttribute(
      'aria-invalid',
      'true',
    );
  });
});
