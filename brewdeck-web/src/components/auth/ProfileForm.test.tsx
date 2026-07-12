import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/lib/api/client';
import { ProfileForm } from './ProfileForm';

const updateProfileMock = vi.fn();
vi.mock('@/lib/auth/AuthProvider', () => ({
  useAuth: () => ({
    user: { id: 1, email: 'brewer@example.com', displayName: 'Old Name', emailVerified: true, createdAt: '' },
    updateProfile: updateProfileMock,
  }),
}));

describe('ProfileForm', () => {
  afterEach(() => vi.clearAllMocks());

  it('prefills the current display name and email', () => {
    render(<ProfileForm />);
    expect(screen.getByLabelText(/display name/i)).toHaveValue('Old Name');
    expect(screen.getByLabelText(/email/i)).toHaveValue('brewer@example.com');
  });

  it('submits a trimmed display name and shows success', async () => {
    updateProfileMock.mockResolvedValue(undefined);
    render(<ProfileForm />);
    const field = screen.getByLabelText(/display name/i);
    await userEvent.clear(field);
    await userEvent.type(field, '  Barista Bob  ');
    await userEvent.click(screen.getByRole('button', { name: /save profile/i }));
    await waitFor(() =>
      expect(updateProfileMock).toHaveBeenCalledWith({ displayName: 'Barista Bob' }),
    );
    expect(await screen.findByText(/profile updated/i)).toBeInTheDocument();
  });

  it('sends null when the display name is cleared', async () => {
    updateProfileMock.mockResolvedValue(undefined);
    render(<ProfileForm />);
    await userEvent.clear(screen.getByLabelText(/display name/i));
    await userEvent.click(screen.getByRole('button', { name: /save profile/i }));
    await waitFor(() => expect(updateProfileMock).toHaveBeenCalledWith({ displayName: null }));
  });

  it('maps server validation errors onto the field', async () => {
    updateProfileMock.mockRejectedValue(
      new ApiError(400, 'Validation failed', '/api/auth/me', {
        displayName: 'Display name must not exceed 100 characters',
      }),
    );
    render(<ProfileForm />);
    await userEvent.type(screen.getByLabelText(/display name/i), 'X');
    await userEvent.click(screen.getByRole('button', { name: /save profile/i }));
    expect(await screen.findByText(/must not exceed 100 characters/i)).toBeInTheDocument();
  });
});
