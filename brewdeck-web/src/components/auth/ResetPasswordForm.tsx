'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import { useSearchParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useFieldError } from '@/i18n/useFieldError';
import { resetPassword } from '@/lib/api/auth';
import { ApiError } from '@/lib/api/client';
import { resetPasswordSchema, type ResetPasswordFormValues } from '@/lib/validation/authSchema';

export function ResetPasswordForm() {
  const t = useTranslations('auth.reset');
  const tc = useTranslations('common');
  const fieldError = useFieldError();
  const searchParams = useSearchParams();
  const token = searchParams.get('token') ?? '';
  const [done, setDone] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordFormValues>({ resolver: zodResolver(resetPasswordSchema) });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    if (!token) {
      setFormError(t('invalidLink'));
      return;
    }
    try {
      await resetPassword({ token, newPassword: values.newPassword });
      setDone(true);
    } catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        setFormError(t('invalidLink'));
        return;
      }
      setFormError(t('failed'));
    }
  });

  if (done) {
    return (
      <Stack spacing={3}>
        <Typography variant="h4" component="h1">
          {t('doneTitle')}
        </Typography>
        <Alert severity="success">{t('done')}</Alert>
        <Button component={NextLink} href="/login" variant="contained" size="large">
          {tc('logIn')}
        </Button>
      </Stack>
    );
  }

  return (
    <Box component="form" onSubmit={onSubmit}>
      <Typography variant="h4" component="h1">
        {t('title')}
      </Typography>
      <Typography color="text.secondary" sx={{ mt: 0.5, mb: 3 }}>
        {t('subtitle')}
      </Typography>
      <Stack spacing={2.25}>
        {formError ? <Alert severity="error">{formError}</Alert> : null}
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
        <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
          {t('submit')}
        </Button>
      </Stack>
    </Box>
  );
}
