'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
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
      <Box sx={{ maxWidth: 400, mx: 'auto', mt: 8 }}>
        <Alert severity="success">
          Your password has been reset. <a href="/login">Log in</a>
        </Alert>
      </Box>
    );
  }

  return (
    <Box component="form" onSubmit={onSubmit} sx={{ maxWidth: 400, mx: 'auto', mt: 8 }}>
      <Typography variant="h5" component="h1" gutterBottom>
        Choose a new password
      </Typography>
      <Stack spacing={2}>
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
        <Button type="submit" variant="contained" disabled={isSubmitting}>
          Reset password
        </Button>
      </Stack>
    </Box>
  );
}
