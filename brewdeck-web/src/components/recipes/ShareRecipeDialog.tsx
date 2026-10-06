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
import { useTranslations } from 'next-intl';
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
  const t = useTranslations('recipes.share');
  const tc = useTranslations('common');
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
      <DialogTitle>{t('title')}</DialogTitle>
      <DialogContent>
        {recipe.shareToken ? (
          <Stack spacing={2} sx={{ mt: 1 }}>
            <DialogContentText>{t('linkInfo')}</DialogContentText>
            <TextField
              label={t('publicLink')}
              value={link}
              slotProps={{ input: { readOnly: true } }}
              fullWidth
            />
            {copyError ? (
              <Alert severity="error">{t('copyFailed')}</Alert>
            ) : null}
          </Stack>
        ) : (
          <DialogContentText sx={{ mt: 1 }}>{t('intro')}</DialogContentText>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{tc('close')}</Button>
        {recipe.shareToken ? (
          <>
            <Button onClick={onCopy}>{tc('copy')}</Button>
            <Button
              color="error"
              onClick={() => unshare.mutate()}
              disabled={unshare.isPending}
              startIcon={unshare.isPending ? <CircularProgress size={16} /> : undefined}
            >
              {t('stop')}
            </Button>
          </>
        ) : (
          <Button
            variant="contained"
            onClick={() => share.mutate()}
            disabled={share.isPending}
            startIcon={share.isPending ? <CircularProgress size={16} /> : undefined}
          >
            {t('create')}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}
