import { describe, expect, it } from 'vitest';
import { palettes } from './tokens';

// WCAG 2.2 relative luminance and contrast ratio (success criterion 1.4.3).
function luminance(hex: string): number {
  const [r, g, b] = [1, 3, 5].map((start) => {
    const channel = parseInt(hex.slice(start, start + 2), 16) / 255;
    return channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contrastRatio(a: string, b: string): number {
  const [lighter, darker] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (lighter + 0.05) / (darker + 0.05);
}

const AA_TEXT = 4.5;

describe('contrastRatio', () => {
  it('matches the WCAG reference values', () => {
    expect(contrastRatio('#000000', '#FFFFFF')).toBeCloseTo(21, 5);
    expect(contrastRatio('#777777', '#FFFFFF')).toBeCloseTo(4.48, 2);
  });
});

describe.each(['light', 'dark'] as const)('%s palette', (mode) => {
  const palette = palettes[mode];
  const surfaces = Object.entries(palette.background);

  it.each(surfaces)('text.primary on background.%s meets WCAG AA', (_, surface) => {
    expect(contrastRatio(palette.text.primary, surface)).toBeGreaterThanOrEqual(AA_TEXT);
  });

  it.each(surfaces)('text.secondary on background.%s meets WCAG AA', (_, surface) => {
    expect(contrastRatio(palette.text.secondary, surface)).toBeGreaterThanOrEqual(AA_TEXT);
  });

  it.each(surfaces)('primary.main on background.%s meets WCAG AA', (_, surface) => {
    expect(contrastRatio(palette.primary.main, surface)).toBeGreaterThanOrEqual(AA_TEXT);
  });

  it.each(['primary', 'secondary'] as const)('%s.contrastText on %s.main meets WCAG AA', (key) => {
    expect(contrastRatio(palette[key].contrastText, palette[key].main)).toBeGreaterThanOrEqual(
      AA_TEXT,
    );
  });
});
