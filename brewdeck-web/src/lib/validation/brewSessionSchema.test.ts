import { describe, expect, it } from 'vitest';
import { brewSessionSchema } from './brewSessionSchema';

describe('brewSessionSchema', () => {
  it('requires a recipeId', () => {
    const r = brewSessionSchema.safeParse({ recipeId: '' });
    expect(r.success).toBe(false);
    if (!r.success) {
      expect(r.error.issues.map((i) => i.message)).toContain('brewSession.recipeRequired');
    }
  });

  it('coerces numeric string inputs to numbers', () => {
    const r = brewSessionSchema.safeParse({ recipeId: '3', actualTemp: '90', rating: '9' });
    expect(r.success).toBe(true);
    if (r.success) {
      expect(r.data.recipeId).toBe(3);
      expect(r.data.actualTemp).toBe(90);
      expect(r.data.rating).toBe(9);
    }
  });

  it('treats blank optional numbers as undefined (no NaN)', () => {
    const r = brewSessionSchema.safeParse({ recipeId: '1', actualTemp: '', rating: '' });
    expect(r.success).toBe(true);
    if (r.success) {
      expect(r.data.actualTemp).toBeUndefined();
      expect(r.data.rating).toBeUndefined();
    }
  });

  it('rejects a rating above 10', () => {
    const r = brewSessionSchema.safeParse({ recipeId: '1', rating: '11' });
    expect(r.success).toBe(false);
    if (!r.success) {
      expect(r.error.issues[0].message).toBe('brewSession.ratingTooHigh');
    }
  });

  it('accepts cold water for cold brew', () => {
    expect(brewSessionSchema.safeParse({ recipeId: '1', actualTemp: '4' }).success).toBe(true);
  });

  it('rejects an actual temperature below 0', () => {
    const r = brewSessionSchema.safeParse({ recipeId: '1', actualTemp: '-1' });
    expect(r.success).toBe(false);
    if (!r.success) {
      expect(r.error.issues[0].message).toBe(
        'brewSession.actualTempTooLow',
      );
    }
  });
});
