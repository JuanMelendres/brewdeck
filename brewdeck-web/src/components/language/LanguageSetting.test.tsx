import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { LanguageSetting } from './LanguageSetting';

const updateLanguage = vi.fn();
const refresh = vi.fn();

vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => ({ updateLanguage }) }));
vi.mock('next/navigation', () => ({ useRouter: () => ({ refresh }) }));

afterEach(() => {
  document.cookie = 'brewdeck-locale=; max-age=0; path=/';
  vi.clearAllMocks();
});

describe('LanguageSetting', () => {
  it('shows the current language as selected, each option named in its own language', () => {
    renderWithTheme(<LanguageSetting />);

    expect(screen.getByRole('button', { name: 'English' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Español' })).toHaveAttribute('lang', 'es');
  });

  it('saves the choice, then re-renders the app in that language', async () => {
    updateLanguage.mockResolvedValue(undefined);
    renderWithTheme(<LanguageSetting />);

    await userEvent.click(screen.getByRole('button', { name: 'Español' }));

    expect(updateLanguage).toHaveBeenCalledWith('ES');
    await waitFor(() => expect(refresh).toHaveBeenCalled());
    expect(document.cookie).toContain('brewdeck-locale=es');
  });

  it('still switches this device when saving fails, and says so', async () => {
    updateLanguage.mockRejectedValue(new Error('boom'));
    renderWithTheme(<LanguageSetting />);

    await userEvent.click(screen.getByRole('button', { name: 'Español' }));

    expect(await screen.findByText(/could not save your language/i)).toBeInTheDocument();
    expect(refresh).toHaveBeenCalled();
  });
});
