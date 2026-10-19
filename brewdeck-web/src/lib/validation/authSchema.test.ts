import { describe, expect, it } from 'vitest';
import {
  changePasswordSchema,
  loginSchema,
  registerSchema,
  resetPasswordSchema,
} from './authSchema';

describe('password length in auth schemas (ADR-018)', () => {
  const email = 'brewer@example.com';

  it('registerSchema accepts 128 characters and rejects 129', () => {
    const fits = 'x'.repeat(128);
    expect(registerSchema.safeParse({ email, password: fits, confirmPassword: fits }).success).toBe(
      true,
    );
    const long = 'x'.repeat(129);
    const result = registerSchema.safeParse({ email, password: long, confirmPassword: long });
    expect(result.success).toBe(false);
    expect(result.error?.issues[0].message).toBe('passwordTooLong');
  });

  it('accepts a multibyte passphrase over 72 bytes', () => {
    const passphrase = 'é'.repeat(64); // 128 bytes, rejected while BCrypt's limit applied
    expect(
      registerSchema.safeParse({ email, password: passphrase, confirmPassword: passphrase }).success,
    ).toBe(true);
  });

  it('registerSchema rejects a confirmation that does not match', () => {
    const result = registerSchema.safeParse({
      email,
      password: 'password1',
      confirmPassword: 'password2',
    });
    expect(result.success).toBe(false);
    expect(result.error?.issues[0].path).toEqual(['confirmPassword']);
    expect(result.error?.issues[0].message).toBe('passwordsDoNotMatch');
  });

  it('changePasswordSchema rejects a new password over 128 characters', () => {
    const long = 'é'.repeat(129);
    const result = changePasswordSchema.safeParse({
      currentPassword: 'password1',
      newPassword: long,
      confirmPassword: long,
    });
    expect(result.success).toBe(false);
    expect(result.error?.issues.some((issue) => issue.path[0] === 'newPassword')).toBe(true);
  });

  it('resetPasswordSchema rejects a new password over 128 characters', () => {
    const long = 'x'.repeat(129);
    expect(
      resetPasswordSchema.safeParse({ newPassword: long, confirmPassword: long }).success,
    ).toBe(false);
  });

  it('still enforces the 8-character minimum', () => {
    expect(registerSchema.safeParse({ email, password: 'short' }).success).toBe(false);
  });
});

describe('email length', () => {
  it('rejects emails longer than the 255-character column', () => {
    const tooLong = `${'a'.repeat(250)}@x.com`;
    expect(loginSchema.safeParse({ email: tooLong, password: 'x' }).success).toBe(false);
    expect(registerSchema.safeParse({ email: tooLong, password: 'password1' }).success).toBe(false);
  });
});
