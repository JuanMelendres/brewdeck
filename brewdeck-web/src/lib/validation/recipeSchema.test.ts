import { describe, expect, it } from 'vitest';
import { recipeSchema } from './recipeSchema';

const valid = { coffeeId: '1', methodId: '2', name: 'AeroPress' };

describe('recipeSchema', () => {
  it('requires coffeeId, methodId and name', () => {
    const r = recipeSchema.safeParse({ coffeeId: '', methodId: '', name: '' });
    expect(r.success).toBe(false);
    if (!r.success) {
      const messages = r.error.issues.map((i) => i.message);
      expect(messages).toContain('Coffee is required');
      expect(messages).toContain('Brew method is required');
      expect(messages).toContain('Name is required');
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

  it('rejects a water temperature below 70', () => {
    const r = recipeSchema.safeParse({ ...valid, waterTemp: '60' });
    expect(r.success).toBe(false);
    if (!r.success) {
      expect(r.error.issues[0].message).toBe('Water temperature must be at least 70 degrees Celsius');
    }
  });
});
