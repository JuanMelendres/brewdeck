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
import { ApiError } from '@/lib/api/client';
import { useAuth } from '@/lib/auth/AuthProvider';
import { profileSchema, type ProfileFormValues } from '@/lib/validation/authSchema';

export function ProfileForm() {
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
      setFormError('Could not update your profile. Please try again.');
    }
  });

  return (
    <Box component="form" onSubmit={onSubmit}>
      <Typography variant="h6" component="h2" gutterBottom>
        Profile
      </Typography>
      <Stack spacing={2}>
        {formError ? <Alert severity="error">{formError}</Alert> : null}
        {saved ? <Alert severity="success">Profile updated.</Alert> : null}
        <TextField label="Email" value={user?.email ?? ''} disabled />
        <TextField
          label="Display name"
          {...register('displayName')}
          error={!!errors.displayName}
          helperText={errors.displayName?.message}
        />
        <Button type="submit" variant="contained" disabled={isSubmitting}>
          Save profile
        </Button>
      </Stack>
    </Box>
  );
}
