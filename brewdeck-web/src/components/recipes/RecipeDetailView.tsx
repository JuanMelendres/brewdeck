'use client';

import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import type { ReactNode } from 'react';
import { useRecipe, useRecipeStats } from '@/hooks/useRecipe';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { StatCard } from '@/components/dashboard/StatCard';

function orDash(value: string | number | null): string {
  if (value === null) {
    return '—';
  }
  const text = String(value);
  return text.trim() !== '' ? text : '—';
}

function formatDate(value: string | null): string {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleDateString();
}

export function RecipeDetailView({ recipeId }: { recipeId: number }) {
  const recipeQuery = useRecipe(recipeId);
  const statsQuery = useRecipeStats(recipeId);

  if (recipeQuery.isLoading && !recipeQuery.data) {
    return <Spinner />;
  }

  if (recipeQuery.isError || !recipeQuery.data) {
    return <ErrorState message="Could not load recipe." onRetry={() => recipeQuery.refetch()} />;
  }

  const recipe = recipeQuery.data;

  const details: Array<{ label: string; value: string }> = [
    { label: 'Coffee', value: recipe.coffeeName },
    { label: 'Method', value: recipe.methodName },
    { label: 'Coffee (g)', value: orDash(recipe.coffeeGrams) },
    { label: 'Water (g)', value: orDash(recipe.waterGrams) },
    { label: 'Ratio', value: orDash(recipe.ratio) },
    { label: 'Grind', value: orDash(recipe.grindSetting) },
    { label: 'Water Temp', value: orDash(recipe.waterTemp) },
    { label: 'Brew Time', value: orDash(recipe.brewTime) },
  ];

  let statsBody: ReactNode;
  if (statsQuery.isLoading && !statsQuery.data) {
    statsBody = <Spinner />;
  } else if (statsQuery.isError || !statsQuery.data) {
    statsBody = (
      <ErrorState
        message="Could not load recipe statistics."
        onRetry={() => statsQuery.refetch()}
      />
    );
  } else {
    const stats = statsQuery.data;
    const cards: Array<{ label: string; value: string | number }> = [
      { label: 'Total Sessions', value: stats.totalSessions },
      {
        label: 'Average Rating',
        value: stats.averageRating === null ? '—' : stats.averageRating.toFixed(1),
      },
      { label: 'Last Brewed', value: formatDate(stats.lastBrewedAt) },
    ];
    statsBody = (
      <Grid container spacing={2}>
        {cards.map((card) => (
          <Grid key={card.label} size={{ xs: 12, sm: 4 }}>
            <StatCard label={card.label} value={card.value} />
          </Grid>
        ))}
      </Grid>
    );
  }

  return (
    <>
      <Button component={NextLink} href="/recipes" size="small" sx={{ mb: 1 }}>
        ← Back to recipes
      </Button>

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
        <Typography variant="h5" component="h1">
          {recipe.name}
        </Typography>
        {recipe.favorite ? <Chip label="Favorite" color="primary" size="small" /> : null}
      </Box>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        {details.map((item) => (
          <Grid key={item.label} size={{ xs: 6, sm: 4, md: 3 }}>
            <StatCard label={item.label} value={item.value} />
          </Grid>
        ))}
      </Grid>

      {recipe.steps ? (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" gutterBottom>
            Steps
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ whiteSpace: 'pre-wrap' }}>
            {recipe.steps}
          </Typography>
        </Box>
      ) : null}

      {recipe.expectedTaste ? (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" gutterBottom>
            Expected taste
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {recipe.expectedTaste}
          </Typography>
        </Box>
      ) : null}

      <Typography variant="h6" component="h2" gutterBottom>
        Brew statistics
      </Typography>
      {statsBody}
    </>
  );
}
