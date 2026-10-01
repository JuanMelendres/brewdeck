import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { ThemeModeSetting } from './ThemeModeSetting';

afterEach(() => window.localStorage.clear());

describe('ThemeModeSetting', () => {
  it('starts on the light theme', () => {
    renderWithTheme(<ThemeModeSetting />);

    expect(screen.getByRole('button', { name: /light/i })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: /dark/i })).toHaveAttribute('aria-pressed', 'false');
  });

  it('switches to the dark theme when Dark is chosen', async () => {
    renderWithTheme(<ThemeModeSetting />);

    await userEvent.click(screen.getByRole('button', { name: /dark/i }));

    expect(screen.getByRole('button', { name: /dark/i })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: /light/i })).toHaveAttribute('aria-pressed', 'false');
  });
});
