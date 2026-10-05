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
import { changePassword } from '@/lib/api/auth';
import { ApiError } from '@/lib/api/client';
import {
  changePasswordSchema,
  type ChangePasswordFormValues,
} from '@/lib/validation/authSchema';

export function ChangePasswordForm() {
  const t = useTranslations('auth.changePassword');
  const tc = useTranslations('common');
  const fieldError = useFieldError();
  const [formError, setFormError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordFormValues>({ resolver: zodResolver(changePasswordSchema) });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setSaved(false);
    try {
      await changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      setSaved(true);
      reset();
    } catch (error) {
      if (error instanceof ApiError && error.validationErrors) {
        for (const [field, message] of Object.entries(error.validationErrors)) {
          setError(field as keyof ChangePasswordFormValues, { message });
        }
        return;
      }
      if (error instanceof ApiError && error.status === 400) {
        // Server rejected the supplied current password (no field-level errors).
        setError('currentPassword', { message: 'currentPasswordIncorrect' });
        return;
      }
      setFormError(t('failed'));
    }
  });

  return (
    <Box component="form" onSubmit={onSubmit}>
      <Typography variant="h6" component="h2" gutterBottom>
        {t('title')}
      </Typography>
      <Stack spacing={2}>
        {formError ? <Alert severity="error">{formError}</Alert> : null}
        {saved ? <Alert severity="success">{t('saved')}</Alert> : null}
        <TextField
          label={t('currentPassword')}
          type="password"
          {...register('currentPassword')}
          error={!!errors.currentPassword}
          helperText={fieldError(errors.currentPassword?.message)}
        />
        <TextField
          label={tc('newPassword')}
          type="password"
          {...register('newPassword')}
          error={!!errors.newPassword}
          helperText={fieldError(errors.newPassword?.message)}
        />
        <TextField
          label={tc('confirmNewPassword')}
          type="password"
          {...register('confirmPassword')}
          error={!!errors.confirmPassword}
          helperText={fieldError(errors.confirmPassword?.message)}
        />
        <Button type="submit" variant="contained" disabled={isSubmitting}>
          {t('submit')}
        </Button>
      </Stack>
    </Box>
  );
}
