import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { UserResponse } from '@/lib/api/types';
import { EmailVerificationBanner } from './EmailVerificationBanner';

const resendMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({ resendVerification: () => resendMock() }));

let mockUser: UserResponse | null = null;
vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => ({ user: mockUser }) }));

const unverified: UserResponse = {
  id: 1,
  email: 'brewer@example.com',
  displayName: null,
  emailVerified: false,
  createdAt: '',
};

describe('EmailVerificationBanner', () => {
  afterEach(() => {
    vi.clearAllMocks();
    mockUser = null;
  });

  it('renders nothing when the user is verified', () => {
    mockUser = { ...unverified, emailVerified: true };
    const { container } = render(<EmailVerificationBanner />);
    expect(container).toBeEmptyDOMElement();
  });

  it('shows the warning when unverified', () => {
    mockUser = unverified;
    render(<EmailVerificationBanner />);
    expect(screen.getByText(/email is not verified/i)).toBeInTheDocument();
  });

  it('resends and shows confirmation', async () => {
    mockUser = unverified;
    resendMock.mockResolvedValue({ message: 'ok' });
    render(<EmailVerificationBanner />);
    await userEvent.click(screen.getByRole('button', { name: /resend link/i }));
    await waitFor(() => expect(resendMock).toHaveBeenCalled());
    expect(await screen.findByText(/verification email sent/i)).toBeInTheDocument();
  });

  it('hides when dismissed', async () => {
    mockUser = unverified;
    render(<EmailVerificationBanner />);
    const buttons = screen.getAllByRole('button');
    // The close button is typically the last button (after "Resend link")
    const closeButton = buttons[buttons.length - 1];
    await userEvent.click(closeButton);
    await waitFor(() => {
      expect(screen.queryByText(/email is not verified/i)).not.toBeInTheDocument();
    });
  });
});
