'use client';

import Link from '@mui/material/Link';
import Stack from '@mui/material/Stack';
import NextLink from 'next/link';
import { useTranslations } from 'next-intl';
import { useLegalStatus } from '@/hooks/useLegalStatus';

/** Footer links to the privacy notice and terms, shown only once they are published (LEGAL-01). */
export function LegalLinks() {
  const t = useTranslations('legal');
  const { published } = useLegalStatus();
  if (!published) {
    return null;
  }
  return (
    <Stack
      component="nav"
      aria-label={t('linksLabel')}
      direction="row"
      spacing={2}
      sx={{ justifyContent: 'center', py: 2 }}
    >
      <Link component={NextLink} href="/privacy" variant="body2" color="text.secondary">
        {t('privacy')}
      </Link>
      <Link component={NextLink} href="/terms" variant="body2" color="text.secondary">
        {t('terms')}
      </Link>
    </Stack>
  );
}
