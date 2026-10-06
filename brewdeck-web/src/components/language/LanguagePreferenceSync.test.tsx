import { afterEach, describe, expect, it, vi } from 'vitest';
import type { Language } from '@/lib/api/types';
import { renderWithTheme } from '@/test/renderWithTheme';
import { LanguagePreferenceSync } from './LanguagePreferenceSync';

const refresh = vi.fn();
let language: Language | null = null;
let spanishEnabled = true;

vi.mock('@/lib/auth/AuthProvider', () => ({ useAuth: () => ({ user: { language } }) }));
vi.mock('@/lib/featureFlags/FeatureFlagProvider', () => ({ useFeatureFlag: () => spanishEnabled }));
vi.mock('next/navigation', () => ({ useRouter: () => ({ refresh }) }));

afterEach(() => {
  language = null;
  spanishEnabled = true;
  document.cookie = 'brewdeck-locale=; max-age=0; path=/';
  vi.clearAllMocks();
});

describe('LanguagePreferenceSync', () => {
  it('switches the device to the account language when they differ', () => {
    language = 'ES';
    renderWithTheme(<LanguagePreferenceSync />);

    expect(document.cookie).toContain('brewdeck-locale=es');
    expect(refresh).toHaveBeenCalledTimes(1);
  });

  it('does nothing when the account language is already shown', () => {
    language = 'EN';
    renderWithTheme(<LanguagePreferenceSync />);

    expect(refresh).not.toHaveBeenCalled();
  });

  it('does nothing without a saved language or while Spanish is off', () => {
    renderWithTheme(<LanguagePreferenceSync />);
    language = 'ES';
    spanishEnabled = false;
    renderWithTheme(<LanguagePreferenceSync />);

    expect(refresh).not.toHaveBeenCalled();
  });

  it('tries only once, so a server that keeps rendering English cannot cause a refresh loop', () => {
    language = 'ES';
    const { rerender } = renderWithTheme(<LanguagePreferenceSync />);
    rerender(<LanguagePreferenceSync />);

    expect(refresh).toHaveBeenCalledTimes(1);
  });
});
