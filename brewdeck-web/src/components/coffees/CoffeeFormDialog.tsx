'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Box from '@mui/material/Box';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Grid from '@mui/material/Grid';
import Slider from '@mui/material/Slider';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { useTranslations } from 'next-intl';
import { useFieldError } from '@/i18n/useFieldError';
import { ApiError } from '@/lib/api/client';
import { coffeeSchema, type CoffeeFormValues } from '@/lib/validation/coffeeSchema';
import { useCreateCoffee, useUpdateCoffee } from '@/hooks/useCoffeeMutations';
import { FormSection } from '@/components/ui/FormSection';
import type { Coffee } from '@/lib/api/types';
import { useNotify } from '@/lib/notifications/NotificationProvider';

type TextFieldSpec = { name: keyof CoffeeFormValues; full?: boolean };

// Grouped by meaning: what the coffee is, where it comes from, and how it tastes.
const SECTIONS: Array<{ title: 'sectionCoffee' | 'sectionOrigin'; fields: TextFieldSpec[] }> = [
  {
    title: 'sectionCoffee',
    fields: [
      { name: 'name', full: true },
      { name: 'brand' },
      { name: 'roastLevel' },
      { name: 'process' },
      { name: 'variety' },
    ],
  },
  {
    title: 'sectionOrigin',
    fields: [
      { name: 'origin' },
      { name: 'region' },
      { name: 'farm' },
      { name: 'producer' },
    ],
  },
];

const NOTE_FIELDS: TextFieldSpec[] = [
  { name: 'notesPrimary' },
  { name: 'notesSecondary' },
];

const SCORE_FIELDS = ['acidityScore', 'bodyScore', 'sweetnessScore', 'bitternessScore'] as const;

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
  const t = useTranslations('coffees.form');
  const tf = useTranslations('coffees.fields');
  const tc = useTranslations('common');
  const fieldError = useFieldError();
  const isEdit = coffee !== undefined;
  const notify = useNotify();
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
      onSuccess: () => {
        notify(isEdit ? t('updated') : t('added'));
        onClose();
      },
      onError: (error: unknown) => {
        if (error instanceof ApiError && error.validationErrors) {
          Object.entries(error.validationErrors).forEach(([field, message]) =>
            setError(field as keyof CoffeeFormValues, { message }),
          );
        } else {
          setServerError(error instanceof Error ? error.message : tc('genericError'));
        }
      },
    };
    if (isEdit && coffee) {
      update.mutate({ id: coffee.id, body: data }, options);
    } else {
      create.mutate(data, options);
    }
  };

  const renderTextField = (
    field: TextFieldSpec,
    extra?: { multiline?: boolean; minRows?: number },
  ) => (
    <TextField
      label={tf(field.name)}
      required={field.name === 'name'}
      size="small"
      fullWidth
      error={Boolean(errors[field.name])}
      helperText={fieldError(errors[field.name]?.message)}
      {...extra}
      {...register(field.name)}
    />
  );

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>{isEdit ? t('editTitle') : t('addTitle')}</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogContent>
          {serverError ? (
            <Alert severity="error" sx={{ mb: 2 }}>
              {serverError}
            </Alert>
          ) : null}
          <Stack spacing={3}>
            {SECTIONS.map((section) => (
              <FormSection key={section.title} title={t(section.title)}>
                {section.fields.map((field) => (
                  <Grid key={field.name} size={{ xs: 12, sm: field.full ? 12 : 6 }}>
                    {renderTextField(field)}
                  </Grid>
                ))}
              </FormSection>
            ))}
            <FormSection title={t('sectionTasting')}>
              {NOTE_FIELDS.map((field) => (
                <Grid key={field.name} size={{ xs: 12, sm: 6 }}>
                  {renderTextField(field)}
                </Grid>
              ))}
              {SCORE_FIELDS.map((name) => (
                <Grid key={name} size={{ xs: 12, sm: 6 }}>
                  <Controller
                    name={name}
                    control={control}
                    render={({ field: { value, onChange } }) => (
                      <Box sx={{ px: 1 }}>
                        <Typography variant="body2" id={`${name}-label`}>
                          {t('scoreLabel', { label: tf(name) })}
                        </Typography>
                        <Slider
                          value={typeof value === 'number' ? value : 3}
                          onChange={(_, next) => onChange(next as number)}
                          step={1}
                          marks
                          min={1}
                          max={5}
                          valueLabelDisplay="auto"
                          aria-labelledby={`${name}-label`}
                          aria-label={tf(name)}
                        />
                      </Box>
                    )}
                  />
                </Grid>
              ))}
              <Grid size={12}>
                {renderTextField({ name: 'description' }, { multiline: true, minRows: 3 })}
              </Grid>
            </FormSection>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={pending}>
            {tc('cancel')}
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={pending}
            startIcon={pending ? <CircularProgress size={16} /> : undefined}
          >
            {isEdit ? tc('save') : tc('create')}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
