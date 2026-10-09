'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import FormControlLabel from '@mui/material/FormControlLabel';
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
import { recipeSchema, type RecipeFormValues } from '@/lib/validation/recipeSchema';
import { useCreateRecipe, useUpdateRecipe } from '@/hooks/useRecipeMutations';
import { useCoffeeOptions, useMethodOptions } from '@/hooks/useResourceOptions';
import { useSuggestRecipe } from '@/hooks/useSuggestRecipe';
import { FeatureFlag } from '@/components/ui/FeatureFlag';
import { FormSection } from '@/components/ui/FormSection';
import type { Recipe } from '@/lib/api/types';
import { useNotify } from '@/lib/notifications/NotificationProvider';

type RecipeFormInput = z.input<typeof recipeSchema>;

type TextFieldSpec = { name: keyof RecipeFormValues; multiline?: boolean; number?: boolean };

// Short brewing parameters sit three to a row on wider screens.
const BREWING_FIELDS: TextFieldSpec[] = [
  { name: 'coffeeGrams', number: true },
  { name: 'waterGrams', number: true },
  { name: 'ratio' },
  { name: 'grindSetting' },
  { name: 'waterTemp', number: true },
  { name: 'brewTime' },
];

const NOTE_FIELDS: TextFieldSpec[] = [
  { name: 'steps', multiline: true },
  { name: 'expectedTaste', multiline: true },
];

function toDefaults(recipe?: Recipe): RecipeFormInput {
  return {
    coffeeId: recipe?.coffeeId ?? '',
    methodId: recipe?.methodId ?? '',
    name: recipe?.name ?? '',
    coffeeGrams: recipe?.coffeeGrams ?? '',
    waterGrams: recipe?.waterGrams ?? '',
    ratio: recipe?.ratio ?? '',
    grindSetting: recipe?.grindSetting ?? '',
    waterTemp: recipe?.waterTemp ?? '',
    brewTime: recipe?.brewTime ?? '',
    steps: recipe?.steps ?? '',
    expectedTaste: recipe?.expectedTaste ?? '',
    favorite: recipe?.favorite ?? false,
  } as RecipeFormInput;
}

export function RecipeFormDialog({
  open,
  recipe,
  onClose,
  initialRationale,
}: {
  open: boolean;
  recipe?: Recipe;
  onClose: () => void;
  initialRationale?: string | null;
}) {
  const t = useTranslations('recipes.form');
  const tf = useTranslations('recipes.fields');
  const tc = useTranslations('common');
  const fieldError = useFieldError();
  const isEdit = recipe !== undefined;
  const notify = useNotify();
  const create = useCreateRecipe();
  const update = useUpdateRecipe();
  const coffeeOptions = useCoffeeOptions();
  const methodOptions = useMethodOptions();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    control,
    register,
    handleSubmit,
    setError,
    setValue,
    watch,
    formState: { errors },
  } = useForm<RecipeFormInput, unknown, RecipeFormValues>({
    resolver: zodResolver(recipeSchema),
    values: toDefaults(recipe),
  });

  const pending = create.isPending || update.isPending;

  const suggestion = useSuggestRecipe();
  const [rationale, setRationale] = useState<string | null>(initialRationale ?? null);
  // Which model and prompt produced the values; saved with the recipe, kept on manual edits.
  const [provenance, setProvenance] = useState({
    aiModel: recipe?.aiModel ?? null,
    aiPromptVersion: recipe?.aiPromptVersion ?? null,
  });
  const [suggestError, setSuggestError] = useState<string | null>(null);

  const coffeeId = watch('coffeeId');
  const methodId = watch('methodId');
  const canSuggest = Boolean(coffeeId) && Boolean(methodId) && !suggestion.isPending;

  const onSuggest = () => {
    setRationale(null);
    setSuggestError(null);
    suggestion.mutate(
      { coffeeId: Number(coffeeId), methodId: Number(methodId) },
      {
        onSuccess: (data) => {
          const set = (name: keyof RecipeFormValues, value: string | number | null) => {
            if (value !== null && value !== undefined) {
              setValue(name, value as never, { shouldValidate: true });
            }
          };
          set('coffeeGrams', data.coffeeGrams);
          set('waterGrams', data.waterGrams);
          set('ratio', data.ratio);
          set('grindSetting', data.grindSetting);
          set('waterTemp', data.waterTemp);
          set('brewTime', data.brewTime);
          set('steps', data.steps);
          setRationale(data.rationale);
          setProvenance({ aiModel: data.aiModel, aiPromptVersion: data.aiPromptVersion });
        },
        onError: () =>
          setSuggestError(t('suggestFailed')),
      },
    );
  };

  const onSubmit = (data: RecipeFormValues) => {
    setServerError(null);
    const mutateOptions = {
      onSuccess: () => {
        notify(isEdit ? t('updated') : t('added'));
        onClose();
      },
      onError: (error: unknown) => {
        if (error instanceof ApiError && error.validationErrors) {
          Object.entries(error.validationErrors).forEach(([field, message]) =>
            setError(field as keyof RecipeFormValues, { message }),
          );
        } else {
          setServerError(error instanceof Error ? error.message : tc('genericError'));
        }
      },
    };
    if (isEdit && recipe) {
      update.mutate({ id: recipe.id, body: { ...data, ...provenance } }, mutateOptions);
    } else {
      create.mutate({ ...data, ...provenance }, mutateOptions);
    }
  };

  const renderTextField = (f: TextFieldSpec) => (
    <TextField
      label={tf(f.name)}
      required={f.name === 'name'}
      type={f.number ? 'number' : 'text'}
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
      <DialogTitle>{isEdit ? t('editTitle') : t('addTitle')}</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogContent>
          {serverError ? (
            <Alert severity="error" sx={{ mb: 2 }}>
              {serverError}
            </Alert>
          ) : null}
          <Stack spacing={3}>
            <FormSection title={t('sectionRecipe')}>
              <Grid size={12}>{renderTextField({ name: 'name' })}</Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Controller
                  name="coffeeId"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      select
                      // A native select always shows its first option, so the label must sit above it.
                      slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
                      label={tf('coffeeId')}
                      required
                      size="small"
                      fullWidth
                      disabled={coffeeOptions.isLoading}
                      value={field.value ?? ''}
                      onChange={field.onChange}
                      error={Boolean(errors.coffeeId)}
                      helperText={fieldError(errors.coffeeId?.message)}
                    >
                      <option value="">{coffeeOptions.isLoading ? tc('loadingEllipsis') : t('selectCoffee')}</option>
                      {(coffeeOptions.data ?? []).map((option) => (
                        <option key={option.id} value={option.id}>
                          {option.name}
                        </option>
                      ))}
                    </TextField>
                  )}
                />
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Controller
                  name="methodId"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      select
                      // A native select always shows its first option, so the label must sit above it.
                      slotProps={{ select: { native: true }, inputLabel: { shrink: true } }}
                      label={tf('methodId')}
                      required
                      size="small"
                      fullWidth
                      disabled={methodOptions.isLoading}
                      value={field.value ?? ''}
                      onChange={field.onChange}
                      error={Boolean(errors.methodId)}
                      helperText={fieldError(errors.methodId?.message)}
                    >
                      <option value="">{methodOptions.isLoading ? tc('loadingEllipsis') : t('selectMethod')}</option>
                      {(methodOptions.data ?? []).map((option) => (
                        <option key={option.id} value={option.id}>
                          {option.name}
                        </option>
                      ))}
                    </TextField>
                  )}
                />
              </Grid>
              <FeatureFlag name="aiRecipeAssistant">
                <Grid size={12} sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                  <Button
                    variant="outlined"
                    sx={{ alignSelf: 'flex-start' }}
                    onClick={onSuggest}
                    disabled={!canSuggest}
                    startIcon={suggestion.isPending ? <CircularProgress size={16} /> : undefined}
                  >
                    {t('suggest')}
                  </Button>
                  {suggestError ? <Alert severity="error">{suggestError}</Alert> : null}
                  {rationale ? <Alert severity="info">{rationale}</Alert> : null}
                </Grid>
              </FeatureFlag>
              <Grid size={12}>
                <Controller
                  name="favorite"
                  control={control}
                  render={({ field }) => (
                    <FormControlLabel
                      control={<Checkbox checked={Boolean(field.value)} onChange={(e) => field.onChange(e.target.checked)} />}
                      label={tf('favorite')}
                    />
                  )}
                />
              </Grid>
            </FormSection>
            <FormSection title={t('sectionBrewing')}>
              {BREWING_FIELDS.map((f) => (
                <Grid key={f.name} size={{ xs: 12, sm: 6, md: 4 }}>
                  {renderTextField(f)}
                </Grid>
              ))}
            </FormSection>
            <FormSection title={t('sectionNotes')}>
              {NOTE_FIELDS.map((f) => (
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
