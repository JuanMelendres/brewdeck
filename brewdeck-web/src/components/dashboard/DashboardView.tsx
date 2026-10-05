'use client';

import CoffeeMakerOutlinedIcon from '@mui/icons-material/CoffeeMakerOutlined';
import CoffeeOutlinedIcon from '@mui/icons-material/CoffeeOutlined';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import StarRoundedIcon from '@mui/icons-material/StarRounded';
import TimerOutlinedIcon from '@mui/icons-material/TimerOutlined';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import { useFormatter, useTranslations } from 'next-intl';
import type { ReactNode } from 'react';
import { useDashboardSummary } from '@/hooks/useDashboardSummary';
import { useAuth } from '@/lib/auth/AuthProvider';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { StatCard } from './StatCard';
import { TopRatedRecipes } from './TopRatedRecipes';
import { MostBrewedRecipes } from './MostBrewedRecipes';
import { MethodUsage } from './MethodUsage';
import { MostUsedCoffees } from './MostUsedCoffees';

function greetingKey(hour: number) {
  if (hour < 12) return 'goodMorning';
  if (hour < 19) return 'goodAfternoon';
  return 'goodEvening';
}

export function DashboardView() {
  const { data, isLoading, isError, refetch } = useDashboardSummary();
  const { user } = useAuth();
  const format = useFormatter();
  const t = useTranslations('dashboard');

  if (isLoading) {
    return <Spinner />;
  }

  if (isError || !data) {
    return <ErrorState message={t('loadFailed')} onRetry={() => refetch()} />;
  }

  const now = new Date();
  const name = user?.displayName;
  const cards: Array<{ label: string; value: string | number; icon: ReactNode }> = [
    { label: t('coffees'), value: data.totalCoffees, icon: <CoffeeOutlinedIcon fontSize="small" /> },
    { label: t('brewMethods'), value: data.totalBrewMethods, icon: <CoffeeMakerOutlinedIcon fontSize="small" /> },
    { label: t('recipes'), value: data.totalRecipes, icon: <MenuBookOutlinedIcon fontSize="small" /> },
    { label: t('favoriteRecipes'), value: data.favoriteRecipes, icon: <FavoriteBorderIcon fontSize="small" /> },
    { label: t('brewSessions'), value: data.totalBrewSessions, icon: <TimerOutlinedIcon fontSize="small" /> },
    {
      label: t('averageRating'),
      value: data.averageSessionRating === null ? '—' : data.averageSessionRating.toFixed(1),
      icon: <StarRoundedIcon fontSize="small" />,
    },
  ];

  return (
    <>
      <Box component="header" sx={{ mb: 3.5 }}>
        <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 500 }}>
          {format.dateTime(now, { weekday: 'long', month: 'long', day: 'numeric' })}
        </Typography>
        <Typography variant="h4" component="h1">
          {name ? t(greetingKey(now.getHours()), { name }) : t('title')}
        </Typography>
      </Box>
      <Grid container spacing={2}>
        {cards.map((card) => (
          <Grid key={card.label} size={{ xs: 6, sm: 4, lg: 2 }}>
            <StatCard label={card.label} value={card.value} icon={card.icon} />
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2.5} sx={{ mt: 2.5 }}>
        <Grid size={{ xs: 12, md: 6 }}>
          <TopRatedRecipes />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <MethodUsage />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <MostBrewedRecipes />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <MostUsedCoffees />
        </Grid>
      </Grid>
    </>
  );
}
