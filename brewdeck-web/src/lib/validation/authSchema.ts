import { z } from 'zod';

// Messages are keys under `validation` in messages/*.json; forms translate them with useFieldError().

// BCrypt only accepts 72 bytes. The limit is on UTF-8 bytes, not characters, so accented letters
// (2 bytes) and emoji (4 bytes) use it up faster. Mirrors @MaxUtf8Bytes(72) on the backend.
const MAX_PASSWORD_BYTES = 72;
const PASSWORD_TOO_LONG = 'passwordTooLong';

export function fitsPasswordByteLimit(value: string): boolean {
  return new TextEncoder().encode(value).length <= MAX_PASSWORD_BYTES;
}

function newPassword(minMessage: string) {
  return z.string().min(8, minMessage).refine(fitsPasswordByteLimit, PASSWORD_TOO_LONG);
}

export const loginSchema = z.object({
  email: z.string().min(1, 'emailRequired').max(255, 'emailTooLong').email('emailInvalid'),
  password: z.string().min(1, 'passwordRequired'),
});

export const registerSchema = z.object({
  email: z.string().min(1, 'emailRequired').max(255, 'emailTooLong').email('emailInvalid'),
  password: newPassword('passwordTooShort'),
});

export const profileSchema = z.object({
  displayName: z.string().max(100, 'displayNameTooLong'),
});

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'currentPasswordRequired'),
    newPassword: newPassword('newPasswordTooShort'),
    confirmPassword: z.string().min(1, 'confirmPasswordRequired'),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'passwordsDoNotMatch',
  });

export const forgotPasswordSchema = z.object({
  email: z.string().min(1, 'emailRequired').max(255, 'emailTooLong').email('emailInvalid'),
});

export const resetPasswordSchema = z
  .object({
    newPassword: newPassword('newPasswordTooShort'),
    confirmPassword: z.string().min(1, 'confirmPasswordRequired'),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'passwordsDoNotMatch',
  });

export type LoginFormValues = z.infer<typeof loginSchema>;
export type RegisterFormValues = z.infer<typeof registerSchema>;
export type ProfileFormValues = z.infer<typeof profileSchema>;
export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;
export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;
