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
import { useTranslations } from 'next-intl';
import { useFieldError } from '@/i18n/useFieldError';
import { ApiError } from '@/lib/api/client';
import {
  brewSessionSchema,
  type BrewSessionFormValues,
} from '@/lib/validation/brewSessionSchema';
import { useCreateBrewSession, useUpdateBrewSession } from '@/hooks/useBrewSessionMutations';
import { useRecipeOptions } from '@/hooks/useResourceOptions';
import { FormSection } from '@/components/ui/FormSection';
import type { BrewSession } from '@/lib/api/types';
import { useNotify } from '@/lib/notifications/NotificationProvider';

type BrewSessionFormInput = z.input<typeof brewSessionSchema>;

type TextFieldSpec = {
  name: keyof BrewSessionFormValues;
  multiline?: boolean;
  number?: boolean;
};

// What was actually brewed, three short parameters to a row on wider screens.
const BREW_FIELDS: TextFieldSpec[] = [
  { name: 'actualGrind' },
  { name: 'actualTemp', number: true },
  { name: 'actualTime' },
];

const RESULT_NOTE_FIELDS: TextFieldSpec[] = [
  { name: 'tasteResult', multiline: true },
  { name: 'adjustmentNotes', multiline: true },
];

function toDefaults(session?: BrewSession, recipeId?: number): BrewSessionFormInput {
  return {
    recipeId: session?.recipeId ?? recipeId ?? '',
    actualGrind: session?.actualGrind ?? '',
    actualTemp: session?.actualTemp ?? '',
    actualTime: session?.actualTime ?? '',
    tasteResult: session?.tasteResult ?? '',
    rating: session?.rating ?? '',
    adjustmentNotes: session?.adjustmentNotes ?? '',
  } as BrewSessionFormInput;
}

export function BrewSessionFormDialog({
  open,
  session,
  recipeId,
  onClose,
}: {
  open: boolean;
  /** The session to edit; without it the dialog logs a new one. */
  session?: BrewSession;
  recipeId?: number;
  onClose: () => void;
}) {
  const t = useTranslations('brewSessions.form');
  const tf = useTranslations('brewSessions.fields');
  const tc = useTranslations('common');
  const fieldError = useFieldError();
  const isEdit = session !== undefined;
  const create = useCreateBrewSession();
  const update = useUpdateBrewSession();
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
    values: toDefaults(session, recipeId),
  });

  const pending = create.isPending || update.isPending;

  const onSubmit = (data: BrewSessionFormValues) => {
    setServerError(null);
    const options = {
      onSuccess: () => {
        notify(isEdit ? t('updated') : t('logged'));
        onClose();
      },
      onError: (error: unknown) => {
        if (error instanceof ApiError && error.validationErrors) {
          Object.entries(error.validationErrors).forEach(([field, message]) =>
            setError(field as keyof BrewSessionFormValues, { message }),
          );
        } else {
          setServerError(error instanceof Error ? error.message : tc('genericError'));
        }
      },
    };
    if (isEdit && session) {
      update.mutate({ id: session.id, body: data }, options);
    } else {
      create.mutate(data, options);
    }
  };

  const renderTextField = (f: TextFieldSpec) => (
    <TextField
      label={tf(f.name)}
      type={f.number ? 'number' : 'text'}
      // Grams and temperatures take decimals; the default step of 1 would flag 92.5 as invalid.
      slotProps={f.number ? { htmlInput: { step: 'any' } } : undefined}
      multiline={f.multiline}
      minRows={f.multiline ? 3 : undefined}
      size="small"
      fullWidth
      error={Boolean(errors[f.name])}
      helperText={fieldError(errors[f.name]?.message)}
      {...register(f.name)}
    />
  );

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>{isEdit ? t('editTitle') : t('title')}</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogContent>
          {serverError ? (
            <Alert severity="error" sx={{ mb: 2 }}>
              {serverError}
            </Alert>
          ) : null}
          <Stack spacing={3}>
            <FormSection title={t('sectionBrew')}>
              <Grid size={12}>
                <Controller
                  name="recipeId"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      select
                      // A native select always shows its first option, so the label must sit above it.
                      slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
                      label={tf('recipeId')}
                      required
                      size="small"
                      fullWidth
                      disabled={recipeOptions.isLoading}
                      value={field.value ?? ''}
                      onChange={field.onChange}
                      error={Boolean(errors.recipeId)}
                      helperText={fieldError(errors.recipeId?.message)}
                    >
                      <option value="">
                        {recipeOptions.isLoading ? tc('loadingEllipsis') : t('selectRecipe')}
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
            <FormSection title={t('sectionResult')}>
              <Grid size={{ xs: 12, sm: 4 }}>
                {renderTextField({ name: 'rating', number: true })}
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
