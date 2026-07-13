import { render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { VerifyEmailView } from './VerifyEmailView';

const verifyEmailMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({ verifyEmail: (t: string) => verifyEmailMock(t) }));

const refreshUserMock = vi.fn().mockResolvedValue(undefined);
vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => ({ refreshUser: refreshUserMock }) }));

let tokenValue: string | null = 'valid-token';
vi.mock('next/navigation', () => ({ useSearchParams: () => ({ get: () => tokenValue }) }));

describe('VerifyEmailView', () => {
  afterEach(() => {
    vi.clearAllMocks();
    tokenValue = 'valid-token';
  });

  it('verifies the token and shows success', async () => {
    verifyEmailMock.mockResolvedValue(undefined);
    render(<VerifyEmailView />);
    await waitFor(() => expect(verifyEmailMock).toHaveBeenCalledWith('valid-token'));
    expect(await screen.findByText(/your email has been verified/i)).toBeInTheDocument();
  });

  it('shows the invalid-link message on failure', async () => {
    verifyEmailMock.mockRejectedValue(new Error('bad'));
    render(<VerifyEmailView />);
    expect(await screen.findByText(/invalid or has expired/i)).toBeInTheDocument();
  });

  it('shows the invalid-link message when no token is present', async () => {
    tokenValue = null;
    render(<VerifyEmailView />);
    expect(await screen.findByText(/invalid or has expired/i)).toBeInTheDocument();
    expect(verifyEmailMock).not.toHaveBeenCalled();
  });
});
