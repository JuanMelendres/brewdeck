import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ThemePreference } from '@/lib/api/types';
import { renderWithTheme } from '@/test/renderWithTheme';
import { ThemePreferenceSync } from './ThemePreferenceSync';

let themePreference: ThemePreference | null = null;

vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => ({ user: { themePreference } }) }));

afterEach(() => {
  window.localStorage.clear();
  document.documentElement.className = '';
  themePreference = null;
});

describe('ThemePreferenceSync', () => {
  it("applies the account's saved dark theme", () => {
    themePreference = 'DARK';
    renderWithTheme(<ThemePreferenceSync />);

    expect(document.documentElement).toHaveClass('dark');
  });

  it('keeps the default light theme when nothing is saved', () => {
    renderWithTheme(<ThemePreferenceSync />);

    expect(document.documentElement).not.toHaveClass('dark');
  });
});
