'use client';

import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import { useTranslations } from 'next-intl';

export function Spinner() {
  const t = useTranslations('common');
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }} role="status" aria-label={t('loading')}>
      <CircularProgress />
    </Box>
  );
}
