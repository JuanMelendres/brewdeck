'use client';

import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import { useTranslations } from 'next-intl';
import { useMethodUsage } from '@/hooks/useMethodUsage';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { radius } from '@/lib/theme/tokens';
import { Count } from './RankedList';

export function MethodUsage() {
  const t = useTranslations('dashboard.methodUsage');
  const { data, isLoading, isError, refetch } = useMethodUsage();

  let body: ReactNode;
  if (isLoading && !data) {
    body = <Spinner />;
  } else if (isError || !data) {
    body = <ErrorState message={t('loadFailed')} onRetry={() => refetch()} />;
  } else if (data.length === 0) {
    body = <EmptyState message={t('empty')} />;
  } else {
    const max = Math.max(...data.map((method) => method.recipeCount), 1);
    body = (
      <Box component="ul" sx={{ listStyle: 'none', m: 0, p: 0, pt: 1, display: 'flex', flexDirection: 'column', gap: 2.25 }}>
        {data.map((method) => (
          <Box component="li" key={method.methodId} sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}>
              <Typography variant="body2" sx={{ fontWeight: 500 }}>
                {method.methodName}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                <Count value={method.recipeCount} kind="recipes" />
              </Typography>
            </Box>
            <Box aria-hidden sx={{ height: 10, borderRadius: radius.pill, bgcolor: 'background.tint', overflow: 'hidden' }}>
              <Box
                sx={{
                  height: '100%',
                  borderRadius: radius.pill,
                  bgcolor: 'secondary.main',
                  width: `${(method.recipeCount / max) * 100}%`,
                }}
              />
            </Box>
          </Box>
        ))}
      </Box>
    );
  }

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ p: 3 }}>
        <Typography variant="h6" component="h2" gutterBottom>
          {t('title')}
        </Typography>
        {body}
      </CardContent>
    </Card>
  );
}
