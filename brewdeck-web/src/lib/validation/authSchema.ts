import { z } from 'zod';

// BCrypt only accepts 72 bytes. The limit is on UTF-8 bytes, not characters, so accented letters
// (2 bytes) and emoji (4 bytes) use it up faster. Mirrors @MaxUtf8Bytes(72) on the backend.
const MAX_PASSWORD_BYTES = 72;
const PASSWORD_TOO_LONG =
  'Password must not exceed 72 bytes (fewer characters if it uses accents or emoji)';

export function fitsPasswordByteLimit(value: string): boolean {
  return new TextEncoder().encode(value).length <= MAX_PASSWORD_BYTES;
}

function newPassword(minMessage: string) {
  return z.string().min(8, minMessage).refine(fitsPasswordByteLimit, PASSWORD_TOO_LONG);
}

export const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
});

export const registerSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
  password: newPassword('Password must be at least 8 characters'),
});

export const profileSchema = z.object({
  displayName: z.string().max(100, 'Display name must not exceed 100 characters'),
});

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Current password is required'),
    newPassword: newPassword('New password must be at least 8 characters'),
    confirmPassword: z.string().min(1, 'Confirm your new password'),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match',
  });

export const forgotPasswordSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email'),
});

export const resetPasswordSchema = z
  .object({
    newPassword: newPassword('New password must be at least 8 characters'),
    confirmPassword: z.string().min(1, 'Confirm your new password'),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match',
  });

export type LoginFormValues = z.infer<typeof loginSchema>;
export type RegisterFormValues = z.infer<typeof registerSchema>;
export type ProfileFormValues = z.infer<typeof profileSchema>;
export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;
export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;
