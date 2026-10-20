'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import FormHelperText from '@mui/material/FormHelperText';
import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import { useTranslations } from 'next-intl';
import { useState, type ReactNode } from 'react';
import { useForm } from 'react-hook-form';
import { register as registerAccount } from '@/lib/api/auth';
import { ApiError } from '@/lib/api/client';
import { rateLimitMessage } from '@/lib/api/errors';
import { useFieldError } from '@/i18n/useFieldError';
import { PasswordField } from '@/components/ui/PasswordField';
import { useLegalStatus } from '@/hooks/useLegalStatus';
import { vk } from '@/i18n/validationKey';
import { registerSchema, type RegisterFormValues } from '@/lib/validation/authSchema';

export function RegisterForm() {
  const t = useTranslations('auth.register');
  const tc = useTranslations('common');
  const fieldError = useFieldError();
  const legal = useLegalStatus();
  const [formError, setFormError] = useState<string | null>(null);
  // The address the confirmation went to; set once signup is accepted.
  const [sentTo, setSentTo] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerSchema) });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    // While the legal texts are published the API requires accepting their current version.
    if (legal.published && !values.acceptTerms) {
      setError('acceptTerms', { message: vk('termsRequired') });
      return;
    }
    try {
      await registerAccount({
        email: values.email,
        password: values.password,
        acceptedTermsVersion: legal.published ? (legal.termsVersion ?? undefined) : undefined,
      });
      setSentTo(values.email);
    } catch (error) {
      // The API also checks the password against its common-password list; show that on the field.
      if (error instanceof ApiError && error.validationErrors) {
        for (const [field, message] of Object.entries(error.validationErrors)) {
          const formField = field === 'acceptedTermsVersion' ? 'acceptTerms' : field;
          setError(formField as keyof RegisterFormValues, { message });
        }
        return;
      }
      setFormError(rateLimitMessage(error) ?? t('failed'));
    }
  });

  if (sentTo) {
    // Same screen whether the email was new or already registered: the email itself says which.
    return (
      <Box>
        <Typography variant="h4" component="h1">
          {t('checkEmailTitle')}
        </Typography>
        <Stack spacing={2.25} sx={{ mt: 2 }}>
          <Alert severity="success">{t('checkEmail', { email: sentTo })}</Alert>
          <Button component={NextLink} href="/login" variant="contained" size="large">
            {t('goToLogin')}
          </Button>
        </Stack>
      </Box>
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
          label={tc('email')}
          type="email"
          {...register('email')}
          error={!!errors.email}
          helperText={fieldError(errors.email?.message)}
        />
        <PasswordField
          label={tc('password')}
          {...register('password')}
          error={!!errors.password}
          helperText={fieldError(errors.password?.message)}
        />
        <PasswordField
          label={tc('confirmPassword')}
          {...register('confirmPassword')}
          error={!!errors.confirmPassword}
          helperText={fieldError(errors.confirmPassword?.message)}
        />
        {legal.published ? (
          <Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              {t.rich('notice', { privacy: legalLink('/privacy') })}
            </Typography>
            <FormControlLabel
              control={<Checkbox {...register('acceptTerms')} />}
              label={
                <Typography variant="body2">
                  {t.rich('acceptTerms', { terms: legalLink('/terms'), privacy: legalLink('/privacy') })}
                </Typography>
              }
            />
            {errors.acceptTerms ? (
              <FormHelperText error>{fieldError(errors.acceptTerms.message)}</FormHelperText>
            ) : null}
          </Box>
        ) : null}
        <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
          {t('submit')}
        </Button>
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
          {t.rich('haveAccount', {
            link: (chunks) => (
              <Link component={NextLink} href="/login" underline="hover" sx={{ fontWeight: 600 }}>
                {chunks}
              </Link>
            ),
          })}
        </Typography>
      </Stack>
    </Box>
  );
}

function legalLink(href: string) {
  function LegalLink(chunks: ReactNode) {
    return (
      <Link component={NextLink} href={href} target="_blank" underline="hover">
        {chunks}
      </Link>
    );
  }
  return LegalLink;
}
