'use client';

import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import { useTranslations } from 'next-intl';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Spinner } from '@/components/ui/Spinner';
import { useLegalStatus } from '@/hooks/useLegalStatus';

/**
 * Renders a legal text (LEGAL-01), but only once the API reports the texts as published: until a
 * lawyer approves them they are drafts, so the page says they are not available yet.
 */
export function LegalDocument({ markdown }: { markdown: string }) {
  const t = useTranslations('legal');
  const { isLoading, published } = useLegalStatus();

  return (
    <Container component="main" maxWidth="md" sx={{ py: { xs: 4, md: 6 } }}>
      <Button component={NextLink} href="/" sx={{ mb: 2 }}>
        {t('back')}
      </Button>
      {isLoading ? <Spinner /> : null}
      {!isLoading && !published ? (
        <Typography color="text.secondary">{t('notAvailable')}</Typography>
      ) : null}
      {published ? (
        <Box
          sx={{
            '& h1': { typography: 'h4', mb: 2 },
            '& h2': { typography: 'h6', mt: 4, mb: 1 },
            '& p, & li': { typography: 'body1', lineHeight: 1.7 },
            '& table': { borderCollapse: 'collapse', width: '100%', my: 2, display: 'block', overflowX: 'auto' },
            '& th, & td': { border: 1, borderColor: 'divider', p: 1, textAlign: 'left', verticalAlign: 'top' },
          }}
        >
          <ReactMarkdown
            remarkPlugins={[remarkGfm]}
            components={{
              a: ({ href, children }) =>
                href?.startsWith('/') ? (
                  <Link component={NextLink} href={href}>
                    {children}
                  </Link>
                ) : (
                  <Link href={href}>{children}</Link>
                ),
            }}
          >
            {markdown}
          </ReactMarkdown>
        </Box>
      ) : null}
    </Container>
  );
}
