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
import { useDeleteBrewMethod } from '@/hooks/useBrewMethodMutations';
import type { BrewMethod } from '@/lib/api/brewMethods';
import { useNotify } from '@/lib/notifications/NotificationProvider';

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
  const notify = useNotify();

  const onConfirm = () => {
    setError(null);
    del.mutate(method.id, {
      onSuccess: () => {
        notify(`Brew method "${method.name}" deleted`);
        onClose();
      },
      // A 409 carries the server's explanation, e.g. "Brew method is used by 2 recipes. ...".
      onError: (e: unknown) => setError(e instanceof Error ? e.message : 'Something went wrong'),
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
