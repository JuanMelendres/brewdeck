'use client';

import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Link from '@mui/material/Link';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import type { ReactNode } from 'react';
import { useMostBrewedRecipes } from '@/hooks/useMostBrewedRecipes';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';

export function MostBrewedRecipes() {
  const { data, isLoading, isError, refetch } = useMostBrewedRecipes(5);

  let body: ReactNode;
  if (isLoading && !data) {
    body = <Spinner />;
  } else if (isError || !data) {
    body = (
      <ErrorState message="Could not load most-brewed recipes." onRetry={() => refetch()} />
    );
  } else if (data.length === 0) {
    body = <EmptyState message="No brew sessions yet." />;
  } else {
    body = (
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>#</TableCell>
            <TableCell>Recipe</TableCell>
            <TableCell align="right">Sessions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {data.map((recipe, index) => (
            <TableRow key={recipe.recipeId}>
              <TableCell>{index + 1}</TableCell>
              <TableCell>
                <Link component={NextLink} href={`/recipes/${recipe.recipeId}`}>
                  {recipe.recipeName}
                </Link>
              </TableCell>
              <TableCell align="right">{recipe.totalSessions}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    );
  }

  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="h6" component="h2" gutterBottom>
          Most Brewed Recipes
        </Typography>
        {body}
      </CardContent>
    </Card>
  );
}
