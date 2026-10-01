import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { ThemeModeSetting } from './ThemeModeSetting';

const updateTheme = vi.fn();

vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => ({ updateTheme }) }));

afterEach(() => {
  window.localStorage.clear();
  vi.clearAllMocks();
});

describe('ThemeModeSetting', () => {
  it('starts on the light theme', () => {
    renderWithTheme(<ThemeModeSetting />);

    expect(screen.getByRole('button', { name: /light/i })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: /dark/i })).toHaveAttribute('aria-pressed', 'false');
  });

  it('switches to dark and saves it to the account', async () => {
    updateTheme.mockResolvedValue(undefined);
    renderWithTheme(<ThemeModeSetting />);

    await userEvent.click(screen.getByRole('button', { name: /dark/i }));

    expect(screen.getByRole('button', { name: /dark/i })).toHaveAttribute('aria-pressed', 'true');
    expect(updateTheme).toHaveBeenCalledWith('DARK');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('keeps the theme and shows an error when saving fails', async () => {
    updateTheme.mockRejectedValue(new Error('boom'));
    renderWithTheme(<ThemeModeSetting />);

    await userEvent.click(screen.getByRole('button', { name: /dark/i }));

    expect(screen.getByRole('button', { name: /dark/i })).toHaveAttribute('aria-pressed', 'true');
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not save your theme/i);
  });
});
