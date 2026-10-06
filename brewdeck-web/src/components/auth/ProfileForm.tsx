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
import { ApiError } from '@/lib/api/client';
import { useAuth } from '@/lib/auth/AuthProvider';
import { profileSchema, type ProfileFormValues } from '@/lib/validation/authSchema';

export function ProfileForm() {
  const t = useTranslations('auth.profile');
  const tc = useTranslations('common');
  const fieldError = useFieldError();
  const { user, updateProfile } = useAuth();
  const [formError, setFormError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { displayName: user?.displayName ?? '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    setSaved(false);
    const trimmed = values.displayName.trim();
    try {
      await updateProfile({ displayName: trimmed === '' ? null : trimmed });
      setSaved(true);
    } catch (error) {
      if (error instanceof ApiError && error.validationErrors) {
        for (const [field, message] of Object.entries(error.validationErrors)) {
          setError(field as keyof ProfileFormValues, { message });
        }
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
        <TextField label={tc('email')} value={user?.email ?? ''} disabled />
        <TextField
          label={t('displayName')}
          {...register('displayName')}
          error={!!errors.displayName}
          helperText={fieldError(errors.displayName?.message)}
        />
        <Button type="submit" variant="contained" disabled={isSubmitting}>
          {t('submit')}
        </Button>
      </Stack>
    </Box>
  );
}
