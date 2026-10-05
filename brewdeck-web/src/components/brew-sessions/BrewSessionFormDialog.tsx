'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Grid from '@mui/material/Grid';
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
import { FormSection } from '@/components/ui/FormSection';
import { useNotify } from '@/lib/notifications/NotificationProvider';

type BrewSessionFormInput = z.input<typeof brewSessionSchema>;

type TextFieldSpec = {
  name: keyof BrewSessionFormValues;
  label: string;
  multiline?: boolean;
  number?: boolean;
};

// What was actually brewed, three short parameters to a row on wider screens.
const BREW_FIELDS: TextFieldSpec[] = [
  { name: 'actualGrind', label: 'Actual Grind' },
  { name: 'actualTemp', label: 'Actual Temp', number: true },
  { name: 'actualTime', label: 'Actual Time' },
];

const RESULT_NOTE_FIELDS: TextFieldSpec[] = [
  { name: 'tasteResult', label: 'Taste Result', multiline: true },
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
  const notify = useNotify();
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
      onSuccess: () => {
        notify('Brew session logged');
        onClose();
      },
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

  const renderTextField = (f: TextFieldSpec) => (
    <TextField
      label={f.label}
      type={f.number ? 'number' : 'text'}
      multiline={f.multiline}
      minRows={f.multiline ? 3 : undefined}
      size="small"
      fullWidth
      error={Boolean(errors[f.name])}
      helperText={errors[f.name]?.message}
      {...register(f.name)}
    />
  );

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Add brew session</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogContent>
          {serverError ? (
            <Alert severity="error" sx={{ mb: 2 }}>
              {serverError}
            </Alert>
          ) : null}
          <Stack spacing={3}>
            <FormSection title="Brew">
              <Grid size={12}>
                <Controller
                  name="recipeId"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      select
                      // A native select always shows its first option, so the label must sit above it.
                      slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
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
              </Grid>
              {BREW_FIELDS.map((f) => (
                <Grid key={f.name} size={{ xs: 12, sm: 4 }}>
                  {renderTextField(f)}
                </Grid>
              ))}
            </FormSection>
            <FormSection title="Result">
              <Grid size={{ xs: 12, sm: 4 }}>
                {renderTextField({ name: 'rating', label: 'Rating', number: true })}
              </Grid>
              {RESULT_NOTE_FIELDS.map((f) => (
                <Grid key={f.name} size={12}>
                  {renderTextField(f)}
                </Grid>
              ))}
            </FormSection>
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
