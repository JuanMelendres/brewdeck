'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslations } from 'next-intl';
import { vk } from '@/i18n/validationKey';
import { useFieldError } from '@/i18n/useFieldError';
import { ApiError } from '@/lib/api/client';
import { brewMethodSchema, type BrewMethodFormValues } from '@/lib/validation/brewMethodSchema';
import { useCreateBrewMethod, useUpdateBrewMethod } from '@/hooks/useBrewMethodMutations';
import type { BrewMethod } from '@/lib/api/brewMethods';
import { useNotify } from '@/lib/notifications/NotificationProvider';

const DUPLICATE_NAME = vk('brewMethod.duplicateName');

function toDefaults(method?: BrewMethod): BrewMethodFormValues {
  return {
    name: method?.name ?? '',
    description: method?.description ?? '',
  };
}

export function BrewMethodFormDialog({
  open,
  method,
  onClose,
}: {
  open: boolean;
  method?: BrewMethod;
  onClose: () => void;
}) {
  const t = useTranslations('brewMethods.form');
  const tc = useTranslations('common');
  const fieldError = useFieldError();
  const isEdit = method !== undefined;
  const notify = useNotify();
  const create = useCreateBrewMethod();
  const update = useUpdateBrewMethod();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<BrewMethodFormValues>({
    resolver: zodResolver(brewMethodSchema),
    values: toDefaults(method),
  });

  const pending = create.isPending || update.isPending;

  const onSubmit = (data: BrewMethodFormValues) => {
    setServerError(null);
    const options = {
      onSuccess: () => {
        notify(isEdit ? t('updated') : t('added'));
        onClose();
      },
      onError: (error: unknown) => {
        if (error instanceof ApiError && error.validationErrors) {
          Object.entries(error.validationErrors).forEach(([field, message]) =>
            setError(field as keyof BrewMethodFormValues, { message }),
          );
        } else if (error instanceof ApiError && error.status === 409) {
          setError('name', { message: DUPLICATE_NAME });
        } else {
          setServerError(error instanceof Error ? error.message : tc('genericError'));
        }
      },
    };
    if (isEdit && method) {
      update.mutate({ id: method.id, body: data }, options);
    } else {
      create.mutate(data, options);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? t('editTitle') : t('addTitle')}</DialogTitle>
      <form onSubmit={handleSubmit(onSubmit)} noValidate>
        <DialogContent>
          {isEdit ? null : (
            <DialogContentText sx={{ mb: 2 }}>
              {t('privateNote')}
            </DialogContentText>
          )}
          {serverError ? (
            <Alert severity="error" sx={{ mb: 2 }}>
              {serverError}
            </Alert>
          ) : null}
          <Stack spacing={2}>
            <TextField
              label={tc('name')}
              required
              size="small"
              fullWidth
              error={Boolean(errors.name)}
              helperText={fieldError(errors.name?.message)}
              {...register('name')}
            />
            <TextField
              label={tc('description')}
              size="small"
              fullWidth
              multiline
              minRows={2}
              error={Boolean(errors.description)}
              helperText={fieldError(errors.description?.message)}
              {...register('description')}
            />
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
