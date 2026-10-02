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
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { rateLimitMessage } from '@/lib/api/errors';
import { useAuth } from '@/lib/auth/AuthProvider';
import { loginSchema, type LoginFormValues } from '@/lib/validation/authSchema';

export function LoginForm() {
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
      setFormError(rateLimitMessage(error) ?? 'Could not log in. Check your email and password.');
    }
  });

  return (
    <Box component="form" onSubmit={onSubmit}>
      <Typography variant="h4" component="h1">
        Log in
      </Typography>
      <Typography color="text.secondary" sx={{ mt: 0.5, mb: 3 }}>
        Welcome back. Your brews are waiting.
      </Typography>
      <Stack spacing={2.25}>
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
        <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Link component={NextLink} href="/forgot-password" variant="body2" underline="hover">
            Forgot password?
          </Link>
        </Box>
        <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
          Log in
        </Button>
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center' }}>
          No account?{' '}
          <Link component={NextLink} href="/register" underline="hover" sx={{ fontWeight: 600 }}>
            Register
          </Link>
        </Typography>
      </Stack>
    </Box>
  );
}
