'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useTranslations } from 'next-intl';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useFieldError } from '@/i18n/useFieldError';
import { PasswordField } from '@/components/ui/PasswordField';
import { requestEmailChange } from '@/lib/api/auth';
import { ApiError } from '@/lib/api/client';
import { useAuth } from '@/lib/auth/AuthProvider';
import { changeEmailSchema, type ChangeEmailFormValues } from '@/lib/validation/authSchema';

/**
 * Starts an email change (ADR-019). The API answers the same whether or not the new address has an
 * account, so the success message never says which; the link in the new inbox finishes the change.
 */
export function ChangeEmailForm() {
  const t = useTranslations('auth.changeEmail');
  const fieldError = useFieldError();
  const { user } = useAuth();
  const [formError, setFormError] = useState<string | null>(null);
  const [sentTo, setSentTo] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ChangeEmailFormValues>({ resolver: zodResolver(changeEmailSchema) });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setSentTo(null);
    try {
      await requestEmailChange(values);
      setSentTo(values.newEmail.trim().toLowerCase());
      reset();
    } catch (error) {
      if (error instanceof ApiError && error.validationErrors) {
        for (const [field, message] of Object.entries(error.validationErrors)) {
          setError(field as keyof ChangeEmailFormValues, { message });
        }
        return;
      }
      if (error instanceof ApiError && error.status === 400) {
        // The only 400 without field errors is a wrong current password.
        setError('currentPassword', { message: 'currentPasswordIncorrect' });
        return;
      }
      if (error instanceof ApiError && error.status === 429) {
        setFormError(error.message);
        return;
      }
      setFormError(t('failed'));
    }
  });

  return (
    <Box component="form" onSubmit={onSubmit} noValidate>
      <Typography variant="h6" component="h2" gutterBottom>
        {t('title')}
      </Typography>
      <Stack spacing={2}>
        {user ? (
          <Typography variant="body2" color="text.secondary">
            {t('current', { email: user.email })}
          </Typography>
        ) : null}
        {formError ? <Alert severity="error">{formError}</Alert> : null}
        {sentTo ? <Alert severity="success">{t('sent', { email: sentTo })}</Alert> : null}
        <TextField
          label={t('newEmail')}
          type="email"
          autoComplete="email"
          {...register('newEmail')}
          error={!!errors.newEmail}
          helperText={fieldError(errors.newEmail?.message)}
        />
        <PasswordField
          label={t('currentPassword')}
          autoComplete="current-password"
          {...register('currentPassword')}
          error={!!errors.currentPassword}
          helperText={fieldError(errors.currentPassword?.message)}
        />
        <Button type="submit" variant="contained" disabled={isSubmitting}>
          {t('submit')}
        </Button>
      </Stack>
    </Box>
  );
}
