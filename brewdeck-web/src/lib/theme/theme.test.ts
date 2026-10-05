import type { CssVarsTheme } from '@mui/material/styles';
import { describe, expect, it } from 'vitest';
import { theme } from './theme';
import { palettes } from './tokens';

describe('theme', () => {
  it.each(['light', 'dark'] as const)('builds the %s color scheme from the tokens', (mode) => {
    // `Theme` only types `colorSchemes` when CSS-variable typings are opted in globally.
    const palette = (theme as unknown as CssVarsTheme).colorSchemes[mode]?.palette;

    expect(palette?.primary.main).toBe(palettes[mode].primary.main);
    expect(palette?.background.default).toBe(palettes[mode].background.default);
    expect(palette?.background.sidebar).toBe(palettes[mode].background.sidebar);
    expect(palette?.background.tint).toBe(palettes[mode].background.tint);
  });

  it('uses the display font for headings and the body font elsewhere', () => {
    expect(theme.typography.h1.fontFamily).toContain('--font-display');
    expect(theme.typography.body1.fontFamily).toContain('--font-body');
  });

  it('uses a 12 px base radius', () => {
    expect(theme.shape.borderRadius).toBe(12);
  });
});
