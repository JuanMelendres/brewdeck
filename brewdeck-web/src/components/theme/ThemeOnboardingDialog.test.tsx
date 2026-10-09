import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ThemePreference } from '@/lib/api/types';
import { renderWithTheme } from '@/test/renderWithTheme';
import { ThemeOnboardingDialog } from './ThemeOnboardingDialog';

const updateTheme = vi.fn();
const updateLanguage = vi.fn();
const refresh = vi.fn();
let themePreference: ThemePreference | null = null;
let spanishEnabled = false;

vi.mock('@/lib/auth/AuthProvider', () => ({
  useAuth: () => ({ user: { themePreference }, updateTheme, updateLanguage }),
}));

vi.mock('@/lib/featureFlags/FeatureFlagProvider', () => ({
  useFeatureFlag: () => spanishEnabled,
}));

vi.mock('next/navigation', () => ({ useRouter: () => ({ refresh }) }));

afterEach(() => {
  window.localStorage.clear();
  document.documentElement.className = '';
  themePreference = null;
  spanishEnabled = false;
  document.cookie = 'brewdeck-locale=; max-age=0; path=/';
  vi.clearAllMocks();
});

describe('ThemeOnboardingDialog', () => {
  it('asks a user who has never chosen, with Light preselected', () => {
    renderWithTheme(<ThemeOnboardingDialog />);

    expect(screen.getByRole('dialog', { name: /light or dark/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /light/i })).toHaveAttribute('aria-pressed', 'true');
  });

  it('does not appear once the user has a saved preference', () => {
    themePreference = 'DARK';
    renderWithTheme(<ThemeOnboardingDialog />);

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('switches the app live and saves the choice on Continue', async () => {
    updateTheme.mockResolvedValue(undefined);
    renderWithTheme(<ThemeOnboardingDialog />);

    await userEvent.click(screen.getByRole('button', { name: /dark/i }));
    expect(document.documentElement).toHaveClass('dark');

    await userEvent.click(screen.getByRole('button', { name: /continue/i }));
    expect(updateTheme).toHaveBeenCalledWith('DARK');
  });

  it('cannot be dismissed with Escape', async () => {
    renderWithTheme(<ThemeOnboardingDialog />);

    await userEvent.keyboard('{Escape}');

    expect(screen.getByRole('dialog', { name: /light or dark/i })).toBeInTheDocument();
  });

  it('explains a failed save and then lets the user continue', async () => {
    updateTheme.mockRejectedValue(new Error('boom'));
    renderWithTheme(<ThemeOnboardingDialog />);

    await userEvent.click(screen.getByRole('button', { name: /continue/i }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/ask again next time/i);

    await userEvent.click(screen.getByRole('button', { name: /continue/i }));
    expect(updateTheme).toHaveBeenCalledTimes(1);
    // The dialog closes with an exit transition.
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('does not ask for a language while Spanish is off', () => {
    renderWithTheme(<ThemeOnboardingDialog />);

    expect(screen.queryByRole('group', { name: /language/i })).not.toBeInTheDocument();
  });

  it('asks for language and theme together while Spanish is on, applying the language live', async () => {
    spanishEnabled = true;
    updateLanguage.mockResolvedValue(undefined);
    updateTheme.mockResolvedValue(undefined);
    renderWithTheme(<ThemeOnboardingDialog />);

    expect(screen.getByRole('dialog', { name: /set up brewdeck/i })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Español' }));
    expect(document.cookie).toContain('brewdeck-locale=es');
    expect(refresh).toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: /continue/i }));
    // The test locale stays English (the cookie only matters to the server render).
    await waitFor(() => expect(updateLanguage).toHaveBeenCalledWith('EN'));
    expect(updateTheme).toHaveBeenCalledWith('LIGHT');
  });
});
