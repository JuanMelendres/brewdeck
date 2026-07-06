'use client';

import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';
import { useMostUsedCoffees } from '@/hooks/useMostUsedCoffees';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';

export function MostUsedCoffees() {
  const { data, isLoading, isError, refetch } = useMostUsedCoffees(5);

  let body: ReactNode;
  if (isLoading && !data) {
    body = <Spinner />;
  } else if (isError || !data) {
    body = <ErrorState message="Could not load most-used coffees." onRetry={() => refetch()} />;
  } else if (data.length === 0) {
    body = <EmptyState message="No coffees used in recipes yet." />;
  } else {
    body = (
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>#</TableCell>
            <TableCell>Coffee</TableCell>
            <TableCell align="right">Recipes</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {data.map((coffee, index) => (
            <TableRow key={coffee.coffeeId}>
              <TableCell>{index + 1}</TableCell>
              <TableCell>{coffee.coffeeName}</TableCell>
              <TableCell align="right">{coffee.recipeCount}</TableCell>
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
          Most Used Coffees
        </Typography>
        {body}
      </CardContent>
    </Card>
  );
}
