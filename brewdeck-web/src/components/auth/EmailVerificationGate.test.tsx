import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/lib/api/client';
import type { UserResponse } from '@/lib/api/types';
import { notifyEmailNotVerified } from '@/lib/auth/emailVerificationSignal';
import { EmailVerificationGate } from './EmailVerificationGate';
import { renderWithTheme } from '@/test/renderWithTheme';

const resendMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({ resendVerification: () => resendMock() }));

const refreshUserMock = vi.fn();
const logoutMock = vi.fn();
let mockUser: UserResponse | null = null;
vi.mock('@/lib/auth/AuthProvider', () => ({
  useAuth: () => ({ user: mockUser, refreshUser: refreshUserMock, logout: logoutMock }),
}));

let mockFlags = { aiRecipeAssistant: false, requireEmailVerification: false, i18nSpanish: false };
let mockFlagStatus: 'loading' | 'ready' | 'error' = 'ready';
vi.mock('@/lib/featureFlags/FeatureFlagProvider', () => ({
  useFeatureFlags: () => ({ flags: mockFlags, status: mockFlagStatus }),
}));

const unverified: UserResponse = {
  id: 1,
  email: 'brewer@example.com',
  displayName: null,
  emailVerified: false,
  role: 'USER',
  themePreference: null, language: null,
  createdAt: '',
};

function renderGate() {
  return renderWithTheme(
    <EmailVerificationGate>
      <p>The app</p>
    </EmailVerificationGate>,
  );
}

describe('EmailVerificationGate', () => {
  afterEach(() => {
    vi.clearAllMocks();
    mockUser = null;
    mockFlags = { aiRecipeAssistant: false, requireEmailVerification: false, i18nSpanish: false };
    mockFlagStatus = 'ready';
  });

  it('shows the app when the flag is off, even for an unverified user', () => {
    mockUser = unverified;
    renderGate();
    expect(screen.getByText('The app')).toBeInTheDocument();
  });

  it('blocks an unverified user when the flag is on', () => {
    mockUser = unverified;
    mockFlags = { ...mockFlags, requireEmailVerification: true, i18nSpanish: false };
    renderGate();

    expect(
      screen.getByRole('heading', { name: /verify your email to continue/i }),
    ).toBeInTheDocument();
    expect(screen.getByText('brewer@example.com')).toBeInTheDocument();
    expect(screen.queryByText('The app')).not.toBeInTheDocument();
  });

  it('lets a verified user through when the flag is on', () => {
    mockUser = { ...unverified, emailVerified: true };
    mockFlags = { ...mockFlags, requireEmailVerification: true, i18nSpanish: false };
    renderGate();
    expect(screen.getByText('The app')).toBeInTheDocument();
  });

  it('does not flash the app for an unverified user while flags are loading', () => {
    mockUser = unverified;
    mockFlagStatus = 'loading';
    renderGate();
    expect(screen.queryByText('The app')).not.toBeInTheDocument();
  });

  it('blocks as soon as the server reports EMAIL_NOT_VERIFIED, even if the cached flag is off', () => {
    mockUser = unverified;
    renderGate();
    expect(screen.getByText('The app')).toBeInTheDocument();

    act(() => notifyEmailNotVerified());

    expect(
      screen.getByRole('heading', { name: /verify your email to continue/i }),
    ).toBeInTheDocument();
  });

  it('resends the verification email', async () => {
    mockUser = unverified;
    mockFlags = { ...mockFlags, requireEmailVerification: true, i18nSpanish: false };
    resendMock.mockResolvedValue({ message: 'Verification email sent.' });
    renderGate();

    await userEvent.click(screen.getByRole('button', { name: /resend verification email/i }));

    expect(resendMock).toHaveBeenCalledTimes(1);
    expect(await screen.findByText(/verification email sent/i)).toBeInTheDocument();
  });

  it('shows the rate-limit message when resending too often', async () => {
    mockUser = unverified;
    mockFlags = { ...mockFlags, requireEmailVerification: true, i18nSpanish: false };
    resendMock.mockRejectedValue(new ApiError(429, 'Too many attempts. Try again in 40 minutes.'));
    renderGate();

    await userEvent.click(screen.getByRole('button', { name: /resend verification email/i }));

    expect(
      await screen.findByText('Too many attempts. Try again in 40 minutes.'),
    ).toBeInTheDocument();
  });

  it('re-checks the account when the user says they verified', async () => {
    mockUser = unverified;
    mockFlags = { ...mockFlags, requireEmailVerification: true, i18nSpanish: false };
    refreshUserMock.mockResolvedValue(undefined);
    renderGate();

    await userEvent.click(screen.getByRole('button', { name: /i've verified my email/i }));

    expect(refreshUserMock).toHaveBeenCalledTimes(1);
    expect(await screen.findByText(/not verified yet/i)).toBeInTheDocument();
  });

  it('logs out', async () => {
    mockUser = unverified;
    mockFlags = { ...mockFlags, requireEmailVerification: true, i18nSpanish: false };
    renderGate();

    await userEvent.click(screen.getByRole('button', { name: /log out/i }));

    expect(logoutMock).toHaveBeenCalledTimes(1);
  });
});
