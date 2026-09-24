import { describe, expect, it } from 'vitest';
import {
  changePasswordSchema,
  fitsPasswordByteLimit,
  registerSchema,
  resetPasswordSchema,
} from './authSchema';

describe('fitsPasswordByteLimit', () => {
  it('counts UTF-8 bytes, not characters', () => {
    expect(fitsPasswordByteLimit('x'.repeat(72))).toBe(true);
    expect(fitsPasswordByteLimit('x'.repeat(73))).toBe(false);
    // "é" is 2 bytes: 37 characters are 74 bytes.
    expect(fitsPasswordByteLimit('é'.repeat(36))).toBe(true);
    expect(fitsPasswordByteLimit('é'.repeat(37))).toBe(false);
  });
});

describe('password byte limit in auth schemas', () => {
  const email = 'brewer@example.com';

  it('registerSchema rejects a password over 72 bytes', () => {
    expect(registerSchema.safeParse({ email, password: 'x'.repeat(72) }).success).toBe(true);
    const result = registerSchema.safeParse({ email, password: 'x'.repeat(73) });
    expect(result.success).toBe(false);
    expect(result.error?.issues[0].message).toMatch(/must not exceed 72 bytes/);
  });

  it('changePasswordSchema rejects a new password over 72 bytes', () => {
    const long = 'é'.repeat(37);
    const result = changePasswordSchema.safeParse({
      currentPassword: 'password1',
      newPassword: long,
      confirmPassword: long,
    });
    expect(result.success).toBe(false);
    expect(result.error?.issues.some((issue) => issue.path[0] === 'newPassword')).toBe(true);
  });

  it('resetPasswordSchema rejects a new password over 72 bytes', () => {
    const long = 'x'.repeat(80);
    expect(
      resetPasswordSchema.safeParse({ newPassword: long, confirmPassword: long }).success,
    ).toBe(false);
  });

  it('still enforces the 8-character minimum', () => {
    expect(registerSchema.safeParse({ email, password: 'short' }).success).toBe(false);
  });
});
