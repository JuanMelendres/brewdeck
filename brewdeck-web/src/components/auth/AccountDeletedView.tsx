'use client';

import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import { useTranslations } from 'next-intl';

/** Where the browser lands after deleting the account (ADR-020). */
export function AccountDeletedView() {
  const t = useTranslations('auth.accountDeleted');
  return (
    <Stack spacing={3}>
      <Typography variant="h4" component="h1">
        {t('title')}
      </Typography>
      <Alert severity="success">{t('body')}</Alert>
      <Button component={NextLink} href="/register" variant="outlined" size="large">
        {t('signUp')}
      </Button>
    </Stack>
  );
}
