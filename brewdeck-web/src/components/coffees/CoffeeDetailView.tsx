'use client';

import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import { useTranslations } from 'next-intl';
import { useCoffee } from '@/hooks/useCoffee';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { StatCard } from '@/components/dashboard/StatCard';
import { CoffeeTastingRadar } from '@/components/coffees/CoffeeTastingRadar';

function orDash(value: string | null): string {
  return value && value.trim() !== '' ? value : '—';
}

export function CoffeeDetailView({ coffeeId }: { coffeeId: number }) {
  const t = useTranslations('coffees.detail');
  const tf = useTranslations('coffees.fields');
  const tc = useTranslations('common');
  const { data: coffee, isLoading, isError, refetch } = useCoffee(coffeeId);

  if (isLoading && !coffee) {
    return <Spinner />;
  }

  if (isError || !coffee) {
    return <ErrorState message={t('loadFailed')} onRetry={() => refetch()} />;
  }

  const details: Array<{ label: string; value: string }> = [
    { label: tf('brand'), value: orDash(coffee.brand) },
    { label: tf('origin'), value: orDash(coffee.origin) },
    { label: tf('region'), value: orDash(coffee.region) },
    { label: tf('farm'), value: orDash(coffee.farm) },
    { label: tf('producer'), value: orDash(coffee.producer) },
    { label: tf('variety'), value: orDash(coffee.variety) },
    { label: tf('process'), value: orDash(coffee.process) },
    { label: t('roast'), value: orDash(coffee.roastLevel) },
  ];

  return (
    <>
      <Button component={NextLink} href="/coffees" size="small" sx={{ mb: 1 }}>
        {t('back')}
      </Button>

      <Typography variant="h5" component="h1" gutterBottom>
        {coffee.name}
      </Typography>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        {details.map((item) => (
          <Grid key={item.label} size={{ xs: 6, sm: 4, md: 3 }}>
            <StatCard label={item.label} value={item.value} />
          </Grid>
        ))}
      </Grid>

      <Box sx={{ mb: 3 }}>
        <CoffeeTastingRadar
          acidity={coffee.acidityScore}
          body={coffee.bodyScore}
          sweetness={coffee.sweetnessScore}
          bitterness={coffee.bitternessScore}
        />
      </Box>

      {coffee.notesPrimary || coffee.notesSecondary ? (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" gutterBottom>
            {t('tastingNotes')}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {[coffee.notesPrimary, coffee.notesSecondary].filter(Boolean).join(' · ')}
          </Typography>
        </Box>
      ) : null}

      {coffee.description ? (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" gutterBottom>
            {tc('description')}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
            {coffee.description}
          </Typography>
        </Box>
      ) : null}
    </>
  );
}
