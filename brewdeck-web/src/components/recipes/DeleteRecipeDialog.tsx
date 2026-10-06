'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import { useState } from 'react';
import { useTranslations } from 'next-intl';
import { useDeleteRecipe } from '@/hooks/useRecipeMutations';
import type { Recipe } from '@/lib/api/types';
import { useNotify } from '@/lib/notifications/NotificationProvider';

export function DeleteRecipeDialog({
  open,
  recipe,
  onClose,
}: {
  open: boolean;
  recipe: Recipe;
  onClose: () => void;
}) {
  const t = useTranslations('recipes.delete');
  const tc = useTranslations('common');
  const del = useDeleteRecipe();
  const [error, setError] = useState<string | null>(null);
  const notify = useNotify();

  const onConfirm = () => {
    setError(null);
    del.mutate(recipe.id, {
      onSuccess: () => {
        notify(t('deleted', { name: recipe.name }));
        onClose();
      },
      onError: (e: unknown) => setError(e instanceof Error ? e.message : tc('genericError')),
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('title')}</DialogTitle>
      <DialogContent>
        {error ? (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        ) : null}
        <DialogContentText>
          {t('confirm', { name: recipe.name })}
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={del.isPending}>
          {tc('cancel')}
        </Button>
        <Button
          color="error"
          variant="contained"
          onClick={onConfirm}
          disabled={del.isPending}
          startIcon={del.isPending ? <CircularProgress size={16} /> : undefined}
        >
          {tc('delete')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
