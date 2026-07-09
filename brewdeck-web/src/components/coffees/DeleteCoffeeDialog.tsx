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
import { useDeleteCoffee } from '@/hooks/useCoffeeMutations';
import type { Coffee } from '@/lib/api/types';

export function DeleteCoffeeDialog({
  open,
  coffee,
  onClose,
}: {
  open: boolean;
  coffee: Coffee;
  onClose: () => void;
}) {
  const del = useDeleteCoffee();
  const [error, setError] = useState<string | null>(null);

  const onConfirm = () => {
    setError(null);
    del.mutate(coffee.id, {
      onSuccess: () => onClose(),
      onError: (e: unknown) => setError(e instanceof Error ? e.message : 'Something went wrong'),
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Delete coffee</DialogTitle>
      <DialogContent>
        {error ? (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        ) : null}
        <DialogContentText>
          Delete coffee &ldquo;{coffee.name}&rdquo;? This cannot be undone.
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
