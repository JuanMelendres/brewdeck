'use client';

import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import { useTranslations } from 'next-intl';
import { useMostUsedCoffees } from '@/hooks/useMostUsedCoffees';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { Count, RankedList } from './RankedList';

export function MostUsedCoffees() {
  const t = useTranslations('dashboard.mostUsed');
  const { data, isLoading, isError, refetch } = useMostUsedCoffees(5);

  let body: ReactNode;
  if (isLoading && !data) {
    body = <Spinner />;
  } else if (isError || !data) {
    body = <ErrorState message={t('loadFailed')} onRetry={() => refetch()} />;
  } else if (data.length === 0) {
    body = <EmptyState message={t('empty')} />;
  } else {
    body = (
      <RankedList
        items={data.map((coffee) => ({
          key: coffee.coffeeId,
          label: coffee.coffeeName,
          href: `/coffees/${coffee.coffeeId}`,
          badge: <Count value={coffee.recipeCount} kind="recipes" />,
        }))}
      />
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
