'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Slider from '@mui/material/Slider';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { ApiError } from '@/lib/api/client';
import { coffeeSchema, type CoffeeFormValues } from '@/lib/validation/coffeeSchema';
import { useCreateCoffee, useUpdateCoffee } from '@/hooks/useCoffeeMutations';
import type { Coffee } from '@/lib/api/types';

const FIELDS: Array<{ name: keyof CoffeeFormValues; label: string }> = [
  { name: 'name', label: 'Name' },
  { name: 'brand', label: 'Brand' },
  { name: 'origin', label: 'Origin' },
  { name: 'region', label: 'Region' },
  { name: 'farm', label: 'Farm' },
  { name: 'producer', label: 'Producer' },
  { name: 'variety', label: 'Variety' },
  { name: 'process', label: 'Process' },
  { name: 'roastLevel', label: 'Roast Level' },
  { name: 'notesPrimary', label: 'Primary Notes' },
  { name: 'notesSecondary', label: 'Secondary Notes' },
  { name: 'description', label: 'Description' },
];

const SCORE_FIELDS: Array<{ name: keyof CoffeeFormValues; label: string }> = [
  { name: 'acidityScore', label: 'Acidity' },
  { name: 'bodyScore', label: 'Body' },
  { name: 'sweetnessScore', label: 'Sweetness' },
  { name: 'bitternessScore', label: 'Bitterness' },
];

function toDefaults(coffee?: Coffee): CoffeeFormValues {
  return {
    name: coffee?.name ?? '',
    brand: coffee?.brand ?? '',
    origin: coffee?.origin ?? '',
    region: coffee?.region ?? '',
    farm: coffee?.farm ?? '',
    producer: coffee?.producer ?? '',
    variety: coffee?.variety ?? '',
    process: coffee?.process ?? '',
    roastLevel: coffee?.roastLevel ?? '',
    notesPrimary: coffee?.notesPrimary ?? '',
    notesSecondary: coffee?.notesSecondary ?? '',
    acidityScore: coffee?.acidityScore ?? 3,
    bodyScore: coffee?.bodyScore ?? 3,
    sweetnessScore: coffee?.sweetnessScore ?? 3,
    bitternessScore: coffee?.bitternessScore ?? 3,
    description: coffee?.description ?? '',
  };
}

export function CoffeeFormDialog({
  open,
  coffee,
  onClose,
}: {
  open: boolean;
  coffee?: Coffee;
  onClose: () => void;
}) {
  const isEdit = coffee !== undefined;
  const create = useCreateCoffee();
  const update = useUpdateCoffee();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    control,
    formState: { errors },
  } = useForm<CoffeeFormValues>({
    resolver: zodResolver(coffeeSchema),
    values: toDefaults(coffee),
  });

  const pending = create.isPending || update.isPending;

  const onSubmit = (data: CoffeeFormValues) => {
    setServerError(null);
    const options = {
      onSuccess: () => onClose(),
      onError: (error: unknown) => {
        if (error instanceof ApiError && error.validationErrors) {
          Object.entries(error.validationErrors).forEach(([field, message]) =>
            setError(field as keyof CoffeeFormValues, { message }),
          );
        } else {
          setServerError(error instanceof Error ? error.message : 'Something went wrong');
        }
      },
    };
    if (isEdit && coffee) {
      update.mutate({ id: coffee.id, body: data }, options);
    } else {
      create.mutate(data, options);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? 'Edit coffee' : 'Add coffee'}</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogContent>
          {serverError ? (
            <Alert severity="error" sx={{ mb: 2 }}>
              {serverError}
            </Alert>
          ) : null}
          <Stack spacing={2}>
            {FIELDS.map((field) => (
              <TextField
                key={field.name}
                label={field.label}
                required={field.name === 'name'}
                size="small"
                fullWidth
                error={Boolean(errors[field.name])}
                helperText={errors[field.name]?.message}
                {...register(field.name)}
              />
            ))}
            <Typography variant="subtitle2" sx={{ mt: 1 }}>
              Tasting profile (1-5)
            </Typography>
            {SCORE_FIELDS.map((field) => (
              <Controller
                key={field.name}
                name={field.name}
                control={control}
                render={({ field: { value, onChange } }) => (
                  <div>
                    <Typography variant="body2" gutterBottom id={`${field.name}-label`}>
                      {field.label}
                    </Typography>
                    <Slider
                      value={typeof value === 'number' ? value : 3}
                      onChange={(_, next) => onChange(next as number)}
                      step={1}
                      marks
                      min={1}
                      max={5}
                      valueLabelDisplay="auto"
                      aria-labelledby={`${field.name}-label`}
                      aria-label={field.label}
                    />
                  </div>
                )}
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
            {isEdit ? 'Save' : 'Create'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
