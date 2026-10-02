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
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { resetPassword } from '@/lib/api/auth';
import { ApiError } from '@/lib/api/client';
import { resetPasswordSchema, type ResetPasswordFormValues } from '@/lib/validation/authSchema';

export function ResetPasswordForm() {
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
      setFormError('This reset link is invalid or has expired.');
      return;
    }
    try {
      await resetPassword({ token, newPassword: values.newPassword });
      setDone(true);
    } catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        setFormError('This reset link is invalid or has expired.');
        return;
      }
      setFormError('Could not reset your password. Please try again.');
    }
  });

  if (done) {
    return (
      <Stack spacing={3}>
        <Typography variant="h4" component="h1">
          Password updated
        </Typography>
        <Alert severity="success">Your password has been reset.</Alert>
        <Button component={NextLink} href="/login" variant="contained" size="large">
          Log in
        </Button>
      </Stack>
    );
  }

  return (
    <Box component="form" onSubmit={onSubmit}>
      <Typography variant="h4" component="h1">
        Choose a new password
      </Typography>
      <Typography color="text.secondary" sx={{ mt: 0.5, mb: 3 }}>
        Enter it twice to confirm.
      </Typography>
      <Stack spacing={2.25}>
        {formError ? <Alert severity="error">{formError}</Alert> : null}
        <TextField
          label="New password"
          type="password"
          {...register('newPassword')}
          error={!!errors.newPassword}
          helperText={errors.newPassword?.message}
        />
        <TextField
          label="Confirm new password"
          type="password"
          {...register('confirmPassword')}
          error={!!errors.confirmPassword}
          helperText={errors.confirmPassword?.message}
        />
        <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
          Reset password
        </Button>
      </Stack>
    </Box>
  );
}
