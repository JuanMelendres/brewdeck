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
import { ApiError } from '@/lib/api/client';
import { useDeleteBrewMethod } from '@/hooks/useBrewMethodMutations';
import type { BrewMethod } from '@/lib/api/brewMethods';

const IN_USE = 'This method is used by one or more recipes. Change or delete those recipes first.';

export function DeleteBrewMethodDialog({
  open,
  method,
  onClose,
}: {
  open: boolean;
  method: BrewMethod;
  onClose: () => void;
}) {
  const del = useDeleteBrewMethod();
  const [error, setError] = useState<string | null>(null);

  const onConfirm = () => {
    setError(null);
    del.mutate(method.id, {
      onSuccess: () => onClose(),
      onError: (e: unknown) => {
        if (e instanceof ApiError && e.status === 409) {
          setError(IN_USE);
        } else {
          setError(e instanceof Error ? e.message : 'Something went wrong');
        }
      },
    });
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Delete brew method</DialogTitle>
      <DialogContent>
        {error ? (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        ) : null}
        <DialogContentText>
          Delete brew method &ldquo;{method.name}&rdquo;? This cannot be undone.
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
