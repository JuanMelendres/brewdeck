'use client';

import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import { useDashboardSummary } from '@/hooks/useDashboardSummary';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { StatCard } from './StatCard';
import { TopRatedRecipes } from './TopRatedRecipes';

export function DashboardView() {
  const { data, isLoading, isError, refetch } = useDashboardSummary();

  if (isLoading) {
    return <Spinner />;
  }

  if (isError || !data) {
    return <ErrorState message="Could not load dashboard summary." onRetry={() => refetch()} />;
  }

  const cards: Array<{ label: string; value: string | number }> = [
    { label: 'Coffees', value: data.totalCoffees },
    { label: 'Brew Methods', value: data.totalBrewMethods },
    { label: 'Recipes', value: data.totalRecipes },
    { label: 'Favorite Recipes', value: data.favoriteRecipes },
    { label: 'Brew Sessions', value: data.totalBrewSessions },
    {
      label: 'Average Rating',
      value: data.averageSessionRating === null ? '—' : data.averageSessionRating.toFixed(1),
    },
  ];

  return (
    <>
      <Typography variant="h5" component="h1" gutterBottom>
        Dashboard
      </Typography>
      <Grid container spacing={2}>
        {cards.map((card) => (
          <Grid key={card.label} size={{ xs: 12, sm: 6, md: 4 }}>
            <StatCard label={card.label} value={card.value} />
          </Grid>
        ))}
      </Grid>

      <Box sx={{ mt: 3 }}>
        <TopRatedRecipes />
      </Box>
    </>
  );
}
