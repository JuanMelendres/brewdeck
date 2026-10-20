import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/lib/api/client';
import { AccountDataSection } from './AccountDataSection';
import { renderWithTheme } from '@/test/renderWithTheme';

const exportMock = vi.fn();
const deleteMock = vi.fn();
vi.mock('@/lib/api/auth', () => ({
  exportAccount: () => exportMock(),
  deleteAccount: (body: unknown) => deleteMock(body),
}));

const clearSession = vi.fn();
vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => ({ clearSession }) }));

const replace = vi.fn();
vi.mock('next/navigation', () => ({ useRouter: () => ({ replace }) }));

describe('AccountDataSection', () => {
  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => 'blob:export');
    URL.revokeObjectURL = vi.fn();
  });
  afterEach(() => vi.clearAllMocks());

  it('downloads the export as a JSON file', async () => {
    exportMock.mockResolvedValue({ profile: { email: 'brewer@example.com' } });
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
    renderWithTheme(<AccountDataSection />);

    await userEvent.click(screen.getByRole('button', { name: /download my data/i }));

    await waitFor(() => expect(click).toHaveBeenCalled());
    expect(URL.createObjectURL).toHaveBeenCalled();
  });

  it('deletes after confirming with the password, then leaves the app', async () => {
    deleteMock.mockResolvedValue(undefined);
    renderWithTheme(<AccountDataSection />);

    await userEvent.click(screen.getByRole('button', { name: /delete my account/i }));
    const confirm = screen.getByRole('button', { name: /delete permanently/i });
    expect(confirm).toBeDisabled();
    await userEvent.type(
      screen.getByLabelText(/^current password$/i, { selector: 'input' }),
      'brew-test-pass-123',
    );
    await userEvent.click(confirm);

    await waitFor(() =>
      expect(deleteMock).toHaveBeenCalledWith({ currentPassword: 'brew-test-pass-123' }),
    );
    expect(clearSession).toHaveBeenCalled();
    expect(replace).toHaveBeenCalledWith('/account-deleted');
  });

  it('marks a wrong password and keeps the session', async () => {
    deleteMock.mockRejectedValue(new ApiError(400, 'Current password is incorrect'));
    renderWithTheme(<AccountDataSection />);

    await userEvent.click(screen.getByRole('button', { name: /delete my account/i }));
    await userEvent.type(
      screen.getByLabelText(/^current password$/i, { selector: 'input' }),
      'wrong-password-123',
    );
    await userEvent.click(screen.getByRole('button', { name: /delete permanently/i }));

    expect(await screen.findByText('Current password is incorrect')).toBeInTheDocument();
    expect(clearSession).not.toHaveBeenCalled();
    expect(replace).not.toHaveBeenCalled();
  });
});
