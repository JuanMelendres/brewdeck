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
import { useForm } from 'react-hook-form';
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
  { name: 'acidity', label: 'Acidity' },
  { name: 'body', label: 'Body' },
  { name: 'sweetness', label: 'Sweetness' },
  { name: 'bitterness', label: 'Bitterness' },
  { name: 'description', label: 'Description' },
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
    acidity: coffee?.acidity ?? '',
    body: coffee?.body ?? '',
    sweetness: coffee?.sweetness ?? '',
    bitterness: coffee?.bitterness ?? '',
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
