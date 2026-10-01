'use client';

import StarRoundedIcon from '@mui/icons-material/StarRounded';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Link from '@mui/material/Link';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import type { ReactNode } from 'react';
import { useTopRatedRecipes } from '@/hooks/useTopRatedRecipes';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';

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
      <Box component="ol" sx={{ listStyle: 'none', m: 0, p: 0, display: 'flex', flexDirection: 'column', gap: 0.5 }}>
        {data.map((recipe, index) => (
          <Box
            component="li"
            key={recipe.recipeId}
            sx={{ display: 'flex', alignItems: 'center', gap: 1.75, py: 1.25, px: 0.5 }}
          >
            <Box
              aria-hidden
              sx={{
                width: 32,
                height: 32,
                flexShrink: 0,
                borderRadius: '50%',
                bgcolor: 'background.tint',
                color: 'primary.main',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 600,
                fontSize: 13,
              }}
            >
              {index + 1}
            </Box>
            <Box sx={{ flexGrow: 1, minWidth: 0 }}>
              <Link
                component={NextLink}
                href={`/recipes/${recipe.recipeId}`}
                underline="hover"
                color="text.primary"
                sx={{ fontWeight: 600 }}
              >
                {recipe.recipeName}
              </Link>
              <Typography variant="caption" color="text.secondary" component="p">
                {recipe.totalSessions} {recipe.totalSessions === 1 ? 'session' : 'sessions'}
              </Typography>
            </Box>
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 0.5,
                px: 1.25,
                py: 0.5,
                borderRadius: 999,
                bgcolor: 'background.tint',
                color: 'secondary.main',
                fontWeight: 600,
                fontSize: 13,
              }}
            >
              <StarRoundedIcon sx={{ fontSize: 15 }} aria-hidden />
              <span>{recipe.averageRating === null ? '—' : recipe.averageRating.toFixed(1)}</span>
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
          Top Rated Recipes
        </Typography>
        {body}
      </CardContent>
    </Card>
  );
}
