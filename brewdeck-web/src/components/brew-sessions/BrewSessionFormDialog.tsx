'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import type { z } from 'zod';
import { ApiError } from '@/lib/api/client';
import {
  brewSessionSchema,
  type BrewSessionFormValues,
} from '@/lib/validation/brewSessionSchema';
import { useCreateBrewSession } from '@/hooks/useBrewSessionMutations';
import { useRecipeOptions } from '@/hooks/useResourceOptions';

type BrewSessionFormInput = z.input<typeof brewSessionSchema>;

const TEXT_FIELDS: Array<{
  name: keyof BrewSessionFormValues;
  label: string;
  multiline?: boolean;
  number?: boolean;
}> = [
  { name: 'actualGrind', label: 'Actual Grind' },
  { name: 'actualTemp', label: 'Actual Temp', number: true },
  { name: 'actualTime', label: 'Actual Time' },
  { name: 'tasteResult', label: 'Taste Result', multiline: true },
  { name: 'rating', label: 'Rating', number: true },
  { name: 'adjustmentNotes', label: 'Adjustment Notes', multiline: true },
];

function toDefaults(recipeId?: number): BrewSessionFormInput {
  return {
    recipeId: recipeId ?? '',
    actualGrind: '',
    actualTemp: '',
    actualTime: '',
    tasteResult: '',
    rating: '',
    adjustmentNotes: '',
  } as BrewSessionFormInput;
}

export function BrewSessionFormDialog({
  open,
  recipeId,
  onClose,
}: {
  open: boolean;
  recipeId?: number;
  onClose: () => void;
}) {
  const create = useCreateBrewSession();
  const recipeOptions = useRecipeOptions();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    control,
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<BrewSessionFormInput, unknown, BrewSessionFormValues>({
    resolver: zodResolver(brewSessionSchema),
    values: toDefaults(recipeId),
  });

  const pending = create.isPending;

  const onSubmit = (data: BrewSessionFormValues) => {
    setServerError(null);
    create.mutate(data, {
      onSuccess: () => onClose(),
      onError: (error: unknown) => {
        if (error instanceof ApiError && error.validationErrors) {
          Object.entries(error.validationErrors).forEach(([field, message]) =>
            setError(field as keyof BrewSessionFormValues, { message }),
          );
        } else {
          setServerError(error instanceof Error ? error.message : 'Something went wrong');
        }
      },
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Add brew session</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogContent>
          {serverError ? (
            <Alert severity="error" sx={{ mb: 2 }}>
              {serverError}
            </Alert>
          ) : null}
          <Stack spacing={2}>
            <Controller
              name="recipeId"
              control={control}
              render={({ field }) => (
                <TextField
                  select
                  slotProps={{ select: { native: true } }}
                  label="Recipe"
                  required
                  size="small"
                  fullWidth
                  disabled={recipeOptions.isLoading}
                  value={field.value ?? ''}
                  onChange={field.onChange}
                  error={Boolean(errors.recipeId)}
                  helperText={errors.recipeId?.message}
                >
                  <option value="">
                    {recipeOptions.isLoading ? 'Loading…' : 'Select a recipe'}
                  </option>
                  {(recipeOptions.data ?? []).map((option) => (
                    <option key={option.id} value={option.id}>
                      {option.name}
                    </option>
                  ))}
                </TextField>
              )}
            />
            {TEXT_FIELDS.map((f) => (
              <TextField
                key={f.name}
                label={f.label}
                type={f.number ? 'number' : 'text'}
                multiline={f.multiline}
                minRows={f.multiline ? 2 : undefined}
                size="small"
                fullWidth
                error={Boolean(errors[f.name])}
                helperText={errors[f.name]?.message}
                {...register(f.name)}
              />
            ))}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={pending}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={pending}
            startIcon={pending ? <CircularProgress size={16} /> : undefined}
          >
            Create
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
