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
import { forgotPassword } from '@/lib/api/auth';
import { forgotPasswordSchema, type ForgotPasswordFormValues } from '@/lib/validation/authSchema';

export function ForgotPasswordForm() {
  const [submitted, setSubmitted] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordFormValues>({ resolver: zodResolver(forgotPasswordSchema) });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await forgotPassword(values);
      setSubmitted(true);
    } catch {
      setFormError('Could not send the reset link. Please try again.');
    }
  });

  return (
    <Box component="form" onSubmit={onSubmit} sx={{ maxWidth: 400, mx: 'auto', mt: 8 }}>
      <Typography variant="h5" component="h1" gutterBottom>
        Reset your password
      </Typography>
      <Stack spacing={2}>
        {formError ? <Alert severity="error">{formError}</Alert> : null}
        {submitted ? (
          <Alert severity="success">
            If that email exists, a reset link has been sent. Check your inbox.
          </Alert>
        ) : null}
        <TextField
          label="Email"
          type="email"
          {...register('email')}
          error={!!errors.email}
          helperText={errors.email?.message}
        />
        <Button type="submit" variant="contained" disabled={isSubmitting}>
          Send reset link
        </Button>
        <Typography variant="body2">
          Remembered it? <a href="/login">Log in</a>
        </Typography>
      </Stack>
    </Box>
  );
}
