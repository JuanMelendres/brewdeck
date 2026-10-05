'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { rateLimitMessage } from '@/lib/api/errors';
import { useAuth } from '@/lib/auth/AuthProvider';
import { useFieldError } from '@/i18n/useFieldError';
import { loginSchema, type LoginFormValues } from '@/lib/validation/authSchema';

export function LoginForm() {
  const t = useTranslations('login');
  const fieldError = useFieldError();
  const { login } = useAuth();
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginSchema) });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await login(values);
      router.push('/dashboard');
    } catch (error) {
      setFormError(rateLimitMessage(error) ?? t('failed'));
    }
  });

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
          label={t('email')}
          type="email"
          {...register('email')}
          error={!!errors.email}
          helperText={fieldError(errors.email?.message)}
        />
        <TextField
          label={t('password')}
          type="password"
          {...register('password')}
          error={!!errors.password}
          helperText={fieldError(errors.password?.message)}
        />
        <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Link component={NextLink} href="/forgot-password" variant="body2" underline="hover">
            {t('forgotPassword')}
          </Link>
        </Box>
        <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
          {t('submit')}
        </Button>
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
          {t.rich('noAccount', {
            link: (chunks) => (
              <Link component={NextLink} href="/register" underline="hover" sx={{ fontWeight: 600 }}>
                {chunks}
              </Link>
            ),
          })}
        </Typography>
      </Stack>
    </Box>
  );
}
