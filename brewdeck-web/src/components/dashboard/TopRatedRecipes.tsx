'use client';

import StarRoundedIcon from '@mui/icons-material/StarRounded';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import { useTopRatedRecipes } from '@/hooks/useTopRatedRecipes';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { Count, RankedList } from './RankedList';

export function TopRatedRecipes() {
  const { data, isLoading, isError, refetch } = useTopRatedRecipes(5);

  let body: ReactNode;
  if (isLoading && !data) {
    body = <Spinner />;
  } else if (isError || !data) {
    body = (
      <ErrorState message="Could not load top-rated recipes." onRetry={() => refetch()} />
    );
  } else if (data.length === 0) {
    body = <EmptyState message="No rated recipes yet." />;
  } else {
    body = (
      <RankedList
        items={data.map((recipe) => ({
          key: recipe.recipeId,
          label: recipe.recipeName,
          href: `/recipes/${recipe.recipeId}`,
          caption: <Count value={recipe.totalSessions} singular="session" plural="sessions" />,
          badge: (
            <>
              <StarRoundedIcon sx={{ fontSize: 15 }} aria-hidden />
              <span>{recipe.averageRating === null ? '—' : recipe.averageRating.toFixed(1)}</span>
            </>
          ),
        }))}
      />
    );
  }

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ p: 3 }}>
        <Typography variant="h6" component="h2" gutterBottom>
          Top Rated Recipes
        </Typography>
        {body}
      </CardContent>
    </Card>
  );
}
