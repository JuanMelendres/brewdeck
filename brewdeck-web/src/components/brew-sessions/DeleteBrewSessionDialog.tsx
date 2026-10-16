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
import { useLocale, useTranslations } from 'next-intl';
import { useDeleteBrewSession } from '@/hooks/useBrewSessionMutations';
import type { BrewSession } from '@/lib/api/types';
import { formatDateTime } from '@/lib/format/dates';
import { useNotify } from '@/lib/notifications/NotificationProvider';

export function DeleteBrewSessionDialog({
  open,
  session,
  onClose,
  onDeleted,
}: {
  open: boolean;
  session: BrewSession;
  onClose: () => void;
  /** Called after the session is gone, e.g. to leave its detail page. */
  onDeleted?: () => void;
}) {
  const t = useTranslations('brewSessions.delete');
  const tc = useTranslations('common');
  const locale = useLocale();
  const del = useDeleteBrewSession();
  const [error, setError] = useState<string | null>(null);
  const notify = useNotify();

  const onConfirm = () => {
    setError(null);
    del.mutate(session.id, {
      onSuccess: () => {
        notify(t('deleted'));
        onClose();
        onDeleted?.();
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
          {t('confirm', {
            recipe: session.recipeName,
            date: formatDateTime(session.brewedAt, undefined, locale),
          })}
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
