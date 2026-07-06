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
import { useMethodUsage } from '@/hooks/useMethodUsage';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';

export function MethodUsage() {
  const { data, isLoading, isError, refetch } = useMethodUsage();

  let body: ReactNode;
  if (isLoading && !data) {
    body = <Spinner />;
  } else if (isError || !data) {
    body = <ErrorState message="Could not load method usage." onRetry={() => refetch()} />;
  } else if (data.length === 0) {
    body = <EmptyState message="No brew methods yet." />;
  } else {
    body = (
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Method</TableCell>
            <TableCell align="right">Recipes</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {data.map((method) => (
            <TableRow key={method.methodId}>
              <TableCell>{method.methodName}</TableCell>
              <TableCell align="right">{method.recipeCount}</TableCell>
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
          Method Usage
        </Typography>
        {body}
      </CardContent>
    </Card>
  );
}
