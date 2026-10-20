'use client';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useState } from 'react';
import { PasswordField } from '@/components/ui/PasswordField';
import { useFieldError } from '@/i18n/useFieldError';
import { deleteAccount, exportAccount } from '@/lib/api/auth';
import { ApiError } from '@/lib/api/client';
import { rateLimitMessage } from '@/lib/api/errors';
import { useAuth } from '@/lib/auth/AuthProvider';

/** Saves `data` as a pretty-printed JSON file through a temporary link. */
function saveJson(data: unknown, filename: string) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

/** ARCO access and cancellation from the account page (ADR-020). */
export function AccountDataSection() {
  const t = useTranslations('auth.accountData');
  const tc = useTranslations('common');
  const fieldError = useFieldError();
  const router = useRouter();
  const { clearSession } = useAuth();
  const [exportError, setExportError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [password, setPassword] = useState('');
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const onExport = async () => {
    setExportError(null);
    setExporting(true);
    try {
      const data = await exportAccount();
      saveJson(data, `brewdeck-export-${new Date().toISOString().slice(0, 10)}.json`);
    } catch (error) {
      setExportError(rateLimitMessage(error) ?? t('exportFailed'));
    } finally {
      setExporting(false);
    }
  };

  const closeDialog = () => {
    setDialogOpen(false);
    setPassword('');
    setDeleteError(null);
    setPasswordError(false);
  };

  const onDelete = async () => {
    setDeleteError(null);
    setPasswordError(false);
    setDeleting(true);
    try {
      await deleteAccount({ currentPassword: password });
      // The server already ended every session; forget it here and leave the app.
      clearSession();
      router.replace('/account-deleted');
    } catch (error) {
      if (error instanceof ApiError && error.status === 400) {
        setPasswordError(true);
      } else {
        setDeleteError(rateLimitMessage(error) ?? t('deleteFailed'));
      }
      setDeleting(false);
    }
  };

  return (
    <Box>
      <Typography variant="h6" component="h2" gutterBottom>
        {t('title')}
      </Typography>
      <Stack spacing={2}>
        <Typography variant="body2" color="text.secondary">
          {t('exportIntro')}
        </Typography>
        {exportError ? <Alert severity="error">{exportError}</Alert> : null}
        <Button variant="outlined" onClick={onExport} disabled={exporting}>
          {t('export')}
        </Button>
        <Typography variant="body2" color="text.secondary">
          {t('deleteIntro')}
        </Typography>
        <Button variant="outlined" color="error" onClick={() => setDialogOpen(true)}>
          {t('delete')}
        </Button>
      </Stack>
      <Dialog open={dialogOpen} onClose={closeDialog} maxWidth="xs" fullWidth>
        <DialogTitle>{t('dialogTitle')}</DialogTitle>
        <DialogContent>
          {deleteError ? (
            <Alert severity="error" sx={{ mb: 2 }}>
              {deleteError}
            </Alert>
          ) : null}
          <DialogContentText sx={{ mb: 2 }}>{t('dialogText')}</DialogContentText>
          <PasswordField
            label={t('currentPassword')}
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            error={passwordError}
            helperText={passwordError ? fieldError('currentPasswordIncorrect') : undefined}
            fullWidth
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog} disabled={deleting}>
            {tc('cancel')}
          </Button>
          <Button color="error" variant="contained" onClick={onDelete} disabled={deleting || !password}>
            {t('confirmDelete')}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
