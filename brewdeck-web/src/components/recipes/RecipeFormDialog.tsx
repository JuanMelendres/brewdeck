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
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import type { z } from 'zod';
import { ApiError } from '@/lib/api/client';
import { recipeSchema, type RecipeFormValues } from '@/lib/validation/recipeSchema';
import { useCreateRecipe, useUpdateRecipe } from '@/hooks/useRecipeMutations';
import { useCoffeeOptions, useMethodOptions } from '@/hooks/useResourceOptions';
import { useSuggestRecipe } from '@/hooks/useSuggestRecipe';
import { FeatureFlag } from '@/components/ui/FeatureFlag';
import type { Recipe } from '@/lib/api/types';

type RecipeFormInput = z.input<typeof recipeSchema>;

const TEXT_FIELDS: Array<{ name: keyof RecipeFormValues; label: string; multiline?: boolean; number?: boolean }> = [
  { name: 'name', label: 'Name' },
  { name: 'coffeeGrams', label: 'Coffee Grams', number: true },
  { name: 'waterGrams', label: 'Water Grams', number: true },
  { name: 'ratio', label: 'Ratio' },
  { name: 'grindSetting', label: 'Grind Setting' },
  { name: 'waterTemp', label: 'Water Temp', number: true },
  { name: 'brewTime', label: 'Brew Time' },
  { name: 'steps', label: 'Steps', multiline: true },
  { name: 'expectedTaste', label: 'Expected Taste', multiline: true },
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
  const isEdit = recipe !== undefined;
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
        },
        onError: () =>
          setSuggestError('AI suggestions are unavailable right now. Please try again later.'),
      },
    );
  };

  const onSubmit = (data: RecipeFormValues) => {
    setServerError(null);
    const mutateOptions = {
      onSuccess: () => onClose(),
      onError: (error: unknown) => {
        if (error instanceof ApiError && error.validationErrors) {
          Object.entries(error.validationErrors).forEach(([field, message]) =>
            setError(field as keyof RecipeFormValues, { message }),
          );
        } else {
          setServerError(error instanceof Error ? error.message : 'Something went wrong');
        }
      },
    };
    if (isEdit && recipe) {
      update.mutate({ id: recipe.id, body: data }, mutateOptions);
    } else {
      create.mutate(data, mutateOptions);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? 'Edit recipe' : 'Add recipe'}</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogContent>
          {serverError ? (
            <Alert severity="error" sx={{ mb: 2 }}>
              {serverError}
            </Alert>
          ) : null}
          <Stack spacing={2}>
            <Controller
              name="coffeeId"
              control={control}
              render={({ field }) => (
                <TextField
                  select
                  slotProps={{ select: { native: true } }}
                  label="Coffee"
                  required
                  size="small"
                  fullWidth
                  disabled={coffeeOptions.isLoading}
                  value={field.value ?? ''}
                  onChange={field.onChange}
                  error={Boolean(errors.coffeeId)}
                  helperText={errors.coffeeId?.message}
                >
                  <option value="">{coffeeOptions.isLoading ? 'Loading…' : 'Select a coffee'}</option>
                  {(coffeeOptions.data ?? []).map((option) => (
                    <option key={option.id} value={option.id}>
                      {option.name}
                    </option>
                  ))}
                </TextField>
              )}
            />
            <Controller
              name="methodId"
              control={control}
              render={({ field }) => (
                <TextField
                  select
                  slotProps={{ select: { native: true } }}
                  label="Brew Method"
                  required
                  size="small"
                  fullWidth
                  disabled={methodOptions.isLoading}
                  value={field.value ?? ''}
                  onChange={field.onChange}
                  error={Boolean(errors.methodId)}
                  helperText={errors.methodId?.message}
                >
                  <option value="">{methodOptions.isLoading ? 'Loading…' : 'Select a brew method'}</option>
                  {(methodOptions.data ?? []).map((option) => (
                    <option key={option.id} value={option.id}>
                      {option.name}
                    </option>
                  ))}
                </TextField>
              )}
            />
            <FeatureFlag name="aiRecipeAssistant">
              <Button
                variant="outlined"
                onClick={onSuggest}
                disabled={!canSuggest}
                startIcon={suggestion.isPending ? <CircularProgress size={16} /> : undefined}
              >
                Suggest with AI
              </Button>
              {suggestError ? <Alert severity="error">{suggestError}</Alert> : null}
              {rationale ? <Alert severity="info">{rationale}</Alert> : null}
            </FeatureFlag>
            {TEXT_FIELDS.map((f) => (
              <TextField
                key={f.name}
                label={f.label}
                required={f.name === 'name'}
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
            <Controller
              name="favorite"
              control={control}
              render={({ field }) => (
                <FormControlLabel
                  control={<Checkbox checked={Boolean(field.value)} onChange={(e) => field.onChange(e.target.checked)} />}
                  label="Favorite"
                />
              )}
            />
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
