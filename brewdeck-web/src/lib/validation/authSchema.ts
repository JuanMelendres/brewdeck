import { z } from 'zod';
import { vk, type ValidationKey } from '@/i18n/validationKey';

// Messages are keys under `validation` in messages/*.json; forms translate them with useFieldError().

// Up to 128 characters (ADR-018: Argon2id has no 72-byte limit). Mirrors @Size(max = 128).
export const MIN_PASSWORD_LENGTH = 15;
export const MAX_PASSWORD_LENGTH = 128;
const PASSWORD_TOO_LONG = vk('passwordTooLong');

function newPassword(minMessage: ValidationKey) {
  // NIST SP 800-63B minimum for a single factor (ADR-018). Login keeps no minimum: older
  // accounts may still have shorter passwords.
  return z.string().min(MIN_PASSWORD_LENGTH, minMessage).max(MAX_PASSWORD_LENGTH, PASSWORD_TOO_LONG);
}

export const loginSchema = z.object({
  email: z.string().min(1, vk('emailRequired')).max(255, vk('emailTooLong')).email(vk('emailInvalid')),
  password: z.string().min(1, vk('passwordRequired')).max(MAX_PASSWORD_LENGTH, PASSWORD_TOO_LONG),
});

export const registerSchema = z
  .object({
    email: z.string().min(1, vk('emailRequired')).max(255, vk('emailTooLong')).email(vk('emailInvalid')),
    password: newPassword(vk('passwordTooShort')),
    confirmPassword: z.string().min(1, vk('confirmPasswordRequired')),
    // Checked in RegisterForm only while the legal texts are published (LEGAL-01).
    acceptTerms: z.boolean().optional(),
  })
  .refine((values) => values.password === values.confirmPassword, {
    path: ['confirmPassword'],
    message: vk('passwordsDoNotMatch'),
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

export const changeEmailSchema = z.object({
  newEmail: z.string().min(1, vk('emailRequired')).max(255, vk('emailTooLong')).email(vk('emailInvalid')),
  currentPassword: z.string().min(1, vk('currentPasswordRequired')).max(MAX_PASSWORD_LENGTH, PASSWORD_TOO_LONG),
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
export type ChangeEmailFormValues = z.infer<typeof changeEmailSchema>;
export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;
