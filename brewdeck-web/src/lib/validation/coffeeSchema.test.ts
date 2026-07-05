import { describe, expect, it } from 'vitest';
import { coffeeSchema } from './coffeeSchema';

describe('coffeeSchema', () => {
  it('rejects an empty name', () => {
    const result = coffeeSchema.safeParse({ name: '' });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Name is required');
    }
  });

  it('accepts a minimal valid object (name only)', () => {
    const result = coffeeSchema.safeParse({ name: 'Mezcla Veracruz' });
    expect(result.success).toBe(true);
  });

  it('rejects a name longer than 120 characters', () => {
    const result = coffeeSchema.safeParse({ name: 'A'.repeat(121) });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].message).toBe('Name must not exceed 120 characters');
    }
  });
});
