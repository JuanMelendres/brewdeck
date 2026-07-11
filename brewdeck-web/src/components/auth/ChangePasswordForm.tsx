'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { changePassword } from '@/lib/api/auth';
import { ApiError } from '@/lib/api/client';
import {
  changePasswordSchema,
  type ChangePasswordFormValues,
} from '@/lib/validation/authSchema';

export function ChangePasswordForm() {
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
        setError('currentPassword', { message: 'Current password is incorrect' });
        return;
      }
      setFormError('Could not change your password. Please try again.');
    }
  });

  return (
    <Box component="form" onSubmit={onSubmit}>
      <Typography variant="h6" component="h2" gutterBottom>
        Change password
      </Typography>
      <Stack spacing={2}>
        {formError ? <Alert severity="error">{formError}</Alert> : null}
        {saved ? <Alert severity="success">Password changed.</Alert> : null}
        <TextField
          label="Current password"
          type="password"
          {...register('currentPassword')}
          error={!!errors.currentPassword}
          helperText={errors.currentPassword?.message}
        />
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
          Change password
        </Button>
      </Stack>
    </Box>
  );
}
