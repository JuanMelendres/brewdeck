import type { ThemePreference } from '@/lib/api/types';

/** MUI color-scheme mode for a saved account preference, and back. */
export type ThemeMode = 'light' | 'dark';

export function toThemeMode(preference: ThemePreference): ThemeMode {
  return preference === 'DARK' ? 'dark' : 'light';
}

export function toThemePreference(mode: ThemeMode): ThemePreference {
  return mode === 'dark' ? 'DARK' : 'LIGHT';
}
