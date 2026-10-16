import { describe, expect, it } from 'vitest';
import { hasAtMostOneDecimal, recipeSchema } from './recipeSchema';

const valid = { coffeeId: '1', methodId: '2', name: 'AeroPress' };

describe('recipeSchema', () => {
  it('requires coffeeId, methodId and name', () => {
    const r = recipeSchema.safeParse({ coffeeId: '', methodId: '', name: '' });
    expect(r.success).toBe(false);
    if (!r.success) {
      const messages = r.error.issues.map((i) => i.message);
      expect(messages).toContain('recipe.coffeeRequired');
      expect(messages).toContain('recipe.methodRequired');
      expect(messages).toContain('recipe.nameRequired');
    }
  });

  it('coerces numeric string inputs to numbers', () => {
    const r = recipeSchema.safeParse({ ...valid, coffeeGrams: '15', waterTemp: '90' });
    expect(r.success).toBe(true);
    if (r.success) {
      expect(r.data.coffeeId).toBe(1);
      expect(r.data.coffeeGrams).toBe(15);
      expect(r.data.waterTemp).toBe(90);
    }
  });

  it('treats blank optional numbers as undefined (no NaN)', () => {
    const r = recipeSchema.safeParse({ ...valid, coffeeGrams: '', waterTemp: '' });
    expect(r.success).toBe(true);
    if (r.success) {
      expect(r.data.coffeeGrams).toBeUndefined();
      expect(r.data.waterTemp).toBeUndefined();
    }
  });

  it('accepts cold water for cold brew', () => {
    expect(recipeSchema.safeParse({ ...valid, waterTemp: '4' }).success).toBe(true);
  });

  it('rejects a water temperature below 0', () => {
    const r = recipeSchema.safeParse({ ...valid, waterTemp: '-1' });
    expect(r.success).toBe(false);
    if (!r.success) {
      expect(r.error.issues[0].message).toBe('recipe.waterTempTooLow');
    }
  });

  it('rejects grams above the database precision (9999.99)', () => {
    expect(recipeSchema.safeParse({ ...valid, coffeeGrams: '9999.99' }).success).toBe(true);
    const r = recipeSchema.safeParse({ ...valid, coffeeGrams: '10000', waterGrams: '10000' });
    expect(r.success).toBe(false);
    if (!r.success) {
      expect(r.error.issues.map((issue) => issue.message)).toEqual([
        'recipe.coffeeGramsTooHigh',
        'recipe.waterGramsTooHigh',
      ]);
    }
  });

  it('keeps one decimal on the water temperature and rejects two', () => {
    const ok = recipeSchema.safeParse({ ...valid, waterTemp: '92.5' });
    expect(ok.success).toBe(true);
    if (ok.success) {
      expect(ok.data.waterTemp).toBe(92.5);
    }

    const tooPrecise = recipeSchema.safeParse({ ...valid, waterTemp: '92.75' });
    expect(tooPrecise.success).toBe(false);
    if (!tooPrecise.success) {
      expect(tooPrecise.error.issues[0].message).toBe('recipe.waterTempOneDecimal');
    }
  });
});

describe('hasAtMostOneDecimal', () => {
  it('accepts whole numbers and one decimal, despite float noise', () => {
    expect(hasAtMostOneDecimal(92)).toBe(true);
    expect(hasAtMostOneDecimal(92.5)).toBe(true);
    // 0.1 + 0.2 is 0.30000000000000004 in floating point, still one decimal.
    expect(hasAtMostOneDecimal(0.1 + 0.2)).toBe(true);
    expect(hasAtMostOneDecimal(92.3)).toBe(true);
    expect(hasAtMostOneDecimal(92.75)).toBe(false);
  });
});
