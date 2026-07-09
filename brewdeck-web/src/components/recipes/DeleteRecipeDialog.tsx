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
import { useDeleteRecipe } from '@/hooks/useRecipeMutations';
import type { Recipe } from '@/lib/api/types';

export function DeleteRecipeDialog({
  open,
  recipe,
  onClose,
}: {
  open: boolean;
  recipe: Recipe;
  onClose: () => void;
}) {
  const del = useDeleteRecipe();
  const [error, setError] = useState<string | null>(null);

  const onConfirm = () => {
    setError(null);
    del.mutate(recipe.id, {
      onSuccess: () => onClose(),
      onError: (e: unknown) => setError(e instanceof Error ? e.message : 'Something went wrong'),
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Delete recipe</DialogTitle>
      <DialogContent>
        {error ? (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        ) : null}
        <DialogContentText>
          Delete recipe &ldquo;{recipe.name}&rdquo;? This cannot be undone.
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={del.isPending}>
          Cancel
        </Button>
        <Button
          color="error"
          variant="contained"
          onClick={onConfirm}
          disabled={del.isPending}
          startIcon={del.isPending ? <CircularProgress size={16} /> : undefined}
        >
          Delete
        </Button>
      </DialogActions>
    </Dialog>
  );
}
