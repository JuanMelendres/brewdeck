import { z } from 'zod';
import { vk, type ValidationKey } from '@/i18n/validationKey';

// Messages are keys under `validation` in messages/*.json; forms translate them with useFieldError().

// BCrypt only accepts 72 bytes. The limit is on UTF-8 bytes, not characters, so accented letters
// (2 bytes) and emoji (4 bytes) use it up faster. Mirrors @MaxUtf8Bytes(72) on the backend.
const MAX_PASSWORD_BYTES = 72;
const PASSWORD_TOO_LONG = vk('passwordTooLong');

export function fitsPasswordByteLimit(value: string): boolean {
  return new TextEncoder().encode(value).length <= MAX_PASSWORD_BYTES;
}

function newPassword(minMessage: ValidationKey) {
  return z.string().min(8, minMessage).refine(fitsPasswordByteLimit, PASSWORD_TOO_LONG);
}

export const loginSchema = z.object({
  email: z.string().min(1, vk('emailRequired')).max(255, vk('emailTooLong')).email(vk('emailInvalid')),
  password: z.string().min(1, vk('passwordRequired')),
});

export const registerSchema = z.object({
  email: z.string().min(1, vk('emailRequired')).max(255, vk('emailTooLong')).email(vk('emailInvalid')),
  password: newPassword(vk('passwordTooShort')),
});

export const profileSchema = z.object({
  displayName: z.string().max(100, vk('displayNameTooLong')),
});

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, vk('currentPasswordRequired')),
    newPassword: newPassword(vk('newPasswordTooShort')),
    confirmPassword: z.string().min(1, vk('confirmPasswordRequired')),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    message: vk('passwordsDoNotMatch'),
  });

export const forgotPasswordSchema = z.object({
  email: z.string().min(1, vk('emailRequired')).max(255, vk('emailTooLong')).email(vk('emailInvalid')),
});

export const resetPasswordSchema = z
  .object({
    newPassword: newPassword(vk('newPasswordTooShort')),
    confirmPassword: z.string().min(1, vk('confirmPasswordRequired')),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    path: ['confirmPassword'],
    message: vk('passwordsDoNotMatch'),
  });

export type LoginFormValues = z.infer<typeof loginSchema>;
export type RegisterFormValues = z.infer<typeof registerSchema>;
export type ProfileFormValues = z.infer<typeof profileSchema>;
export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;
export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;
