import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Recipe } from '@/lib/api/types';

const textCalls: unknown[][] = [];
const saveMock = vi.fn();

vi.mock('jspdf', () => ({
  jsPDF: class {
    internal = { pageSize: { getWidth: () => 210 } };
    setFontSize = vi.fn();
    splitTextToSize = (text: string) => String(text).split('\n');
    save = saveMock;
    text = (...args: unknown[]) => {
      textCalls.push(args);
    };
  },
}));

import { buildRecipePdf, downloadRecipePdf, orDash, recipePdfFilename } from './recipePdf';

function emittedText(): string[] {
  return textCalls
    .map((args) => args[0])
    .flatMap((value) => (Array.isArray(value) ? value : [value]))
    .map((value) => String(value));
}

const fullRecipe: Recipe = {
  id: 1,
  coffeeId: 1,
  coffeeName: 'Mezcla Veracruz',
  methodId: 1,
  methodName: 'AeroPress',
  name: 'Mezcla AeroPress',
  coffeeGrams: 15,
  waterGrams: 230,
  ratio: '1:15',
  grindSetting: 'Timemore S3 - 5.5',
  waterTemp: 90,
  brewTime: '2:30',
  steps: 'Bloom 30s, stir gently, press slowly.',
  expectedTaste: 'Clean, aromatic, balanced.',
  favorite: true,
  createdAt: '2026-01-01T00:00:00',
  updatedAt: null,
  shareToken: null,
};

const emptyRecipe: Recipe = {
  ...fullRecipe,
  name: 'Bare Recipe',
  coffeeGrams: null,
  waterGrams: null,
  ratio: null,
  grindSetting: null,
  waterTemp: null,
  brewTime: null,
  steps: null,
  expectedTaste: null,
  favorite: false,
};

beforeEach(() => {
  textCalls.length = 0;
});

describe('orDash', () => {
  it('returns an em dash for null and blank, and the value otherwise', () => {
    expect(orDash(null)).toBe('—');
    expect(orDash('   ')).toBe('—');
    expect(orDash(90)).toBe('90');
    expect(orDash('1:15')).toBe('1:15');
  });
});

describe('recipePdfFilename', () => {
  it('slugifies the recipe name and appends .pdf', () => {
    expect(recipePdfFilename(fullRecipe)).toBe('mezcla-aeropress.pdf');
  });

  it('falls back to recipe.pdf when the name has no alphanumerics', () => {
    expect(recipePdfFilename({ ...fullRecipe, name: '!!!' })).toBe('recipe.pdf');
  });
});

describe('buildRecipePdf', () => {
  it('emits the header, title, coffee, method, and every param', () => {
    buildRecipePdf(fullRecipe);
    const text = emittedText();

    expect(text).toContain('BrewDeck');
    expect(text).toContain('Mezcla AeroPress');
    expect(text).toContain('Coffee: Mezcla Veracruz');
    expect(text).toContain('Method: AeroPress');
    expect(text).toContain('Coffee (g): 15');
    expect(text).toContain('Water (g): 230');
    expect(text).toContain('Ratio: 1:15');
    expect(text).toContain('Grind: Timemore S3 - 5.5');
    expect(text).toContain('Water Temp: 90');
    expect(text).toContain('Brew Time: 2:30');
  });

  it('renders an em dash for null params', () => {
    buildRecipePdf(emptyRecipe);
    const text = emittedText();

    expect(text).toContain('Coffee (g): —');
    expect(text).toContain('Ratio: —');
    expect(text).toContain('Water Temp: —');
  });

  it('shows the Favorite badge only when the recipe is a favorite', () => {
    buildRecipePdf(fullRecipe);
    expect(emittedText()).toContain('Favorite');

    textCalls.length = 0;
    buildRecipePdf({ ...fullRecipe, favorite: false });
    expect(emittedText()).not.toContain('Favorite');
  });

  it('includes the Steps and Expected taste sections when present', () => {
    buildRecipePdf(fullRecipe);
    const text = emittedText();

    expect(text).toContain('Steps');
    expect(text).toContain('Bloom 30s, stir gently, press slowly.');
    expect(text).toContain('Expected taste');
    expect(text).toContain('Clean, aromatic, balanced.');
  });

  it('omits the Steps and Expected taste sections when those fields are null', () => {
    buildRecipePdf(emptyRecipe);
    const text = emittedText();

    expect(text).not.toContain('Steps');
    expect(text).not.toContain('Expected taste');
  });

  it('returns a jsPDF document', () => {
    const doc = buildRecipePdf(fullRecipe);
    expect(typeof doc.save).toBe('function');
  });
});

describe('downloadRecipePdf', () => {
  it('saves the document under the slugified filename', () => {
    downloadRecipePdf(fullRecipe);
    expect(saveMock).toHaveBeenCalledWith('mezcla-aeropress.pdf');
  });
});
