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
import { useState } from 'react';
import type { Recipe } from '@/lib/api/types';
import { useShareRecipe, useUnshareRecipe } from '@/hooks/useShareRecipe';

export function ShareRecipeDialog({
  open,
  recipe,
  onClose,
}: {
  open: boolean;
  recipe: Recipe;
  onClose: () => void;
}) {
  const share = useShareRecipe(recipe.id);
  const unshare = useUnshareRecipe(recipe.id);
  const [copyError, setCopyError] = useState(false);

  const origin = typeof window === 'undefined' ? '' : window.location.origin;
  const link = recipe.shareToken ? `${origin}/share/${recipe.shareToken}` : '';

  const onCopy = async () => {
    setCopyError(false);
    try {
      await navigator.clipboard.writeText(link);
    } catch {
      setCopyError(true);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>Share recipe</DialogTitle>
      <DialogContent>
        {recipe.shareToken ? (
          <Stack spacing={2} sx={{ mt: 1 }}>
            <DialogContentText>
              Anyone with this link can view a read-only copy of this recipe.
            </DialogContentText>
            <TextField
              label="Public link"
              value={link}
              slotProps={{ input: { readOnly: true } }}
              fullWidth
            />
            {copyError ? (
              <Alert severity="error">Couldn&apos;t copy — copy it manually.</Alert>
            ) : null}
          </Stack>
        ) : (
          <DialogContentText sx={{ mt: 1 }}>
            Create a public link to let anyone view this recipe. You can stop sharing at any time.
          </DialogContentText>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
        {recipe.shareToken ? (
          <>
            <Button onClick={onCopy}>Copy</Button>
            <Button
              color="error"
              onClick={() => unshare.mutate()}
              disabled={unshare.isPending}
              startIcon={unshare.isPending ? <CircularProgress size={16} /> : undefined}
            >
              Stop sharing
            </Button>
          </>
        ) : (
          <Button
            variant="contained"
            onClick={() => share.mutate()}
            disabled={share.isPending}
            startIcon={share.isPending ? <CircularProgress size={16} /> : undefined}
          >
            Create link
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
