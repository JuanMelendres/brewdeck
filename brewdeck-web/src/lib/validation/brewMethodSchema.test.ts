import { describe, expect, it } from 'vitest';
import { brewMethodSchema } from './brewMethodSchema';

describe('brewMethodSchema', () => {
  it('accepts a name with an optional description', () => {
    expect(brewMethodSchema.safeParse({ name: 'Clever Dripper' }).success).toBe(true);
    expect(brewMethodSchema.safeParse({ name: 'Moka', description: 'Stovetop' }).success).toBe(true);
  });

  it('rejects a blank or whitespace-only name', () => {
    expect(brewMethodSchema.safeParse({ name: '' }).success).toBe(false);
    expect(brewMethodSchema.safeParse({ name: '   ' }).success).toBe(false);
  });

  it('mirrors the backend length limits', () => {
    expect(brewMethodSchema.safeParse({ name: 'x'.repeat(80) }).success).toBe(true);
    expect(brewMethodSchema.safeParse({ name: 'x'.repeat(81) }).success).toBe(false);
    expect(brewMethodSchema.safeParse({ name: 'ok', description: 'x'.repeat(501) }).success).toBe(false);
  });
});
