'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useAuth } from '@/lib/auth/AuthProvider';
import { registerSchema, type RegisterFormValues } from '@/lib/validation/authSchema';

export function RegisterForm() {
  const { register: registerAccount } = useAuth();
  const router = useRouter();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerSchema) });

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null);
    try {
      await registerAccount(values);
      router.push('/dashboard');
    } catch {
      setFormError('Could not register. That email may already be in use.');
    }
  });

  return (
    <Box component="form" onSubmit={onSubmit} sx={{ maxWidth: 400, mx: 'auto', mt: 8 }}>
      <Typography variant="h5" component="h1" gutterBottom>
        Register
      </Typography>
      <Stack spacing={2}>
        {formError ? <Alert severity="error">{formError}</Alert> : null}
        <TextField
          label="Email"
          type="email"
          {...register('email')}
          error={!!errors.email}
          helperText={errors.email?.message}
        />
        <TextField
          label="Password"
          type="password"
          {...register('password')}
          error={!!errors.password}
          helperText={errors.password?.message}
        />
        <Button type="submit" variant="contained" disabled={isSubmitting}>
          Create account
        </Button>
        <Typography variant="body2">
          Already have an account? <a href="/login">Log in</a>
        </Typography>
      </Stack>
    </Box>
  );
}
