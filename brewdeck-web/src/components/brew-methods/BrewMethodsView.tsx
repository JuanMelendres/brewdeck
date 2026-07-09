'use client';

import TablePagination from '@mui/material/TablePagination';
import Typography from '@mui/material/Typography';
import { useState, type ReactNode } from 'react';
import { useBrewMethods } from '@/hooks/useBrewMethods';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { BrewMethodsTable } from './BrewMethodsTable';

export function BrewMethodsView() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const { data, isLoading, isError, refetch } = useBrewMethods({ page, size });

  let body: ReactNode;
  if (isLoading && !data) {
    body = <Spinner />;
  } else if (isError || !data) {
    body = <ErrorState message="Could not load brew methods." onRetry={() => refetch()} />;
  } else if (data.content.length === 0) {
    body = <EmptyState message="No brew methods found." />;
  } else {
    body = (
      <>
        <BrewMethodsTable methods={data.content} />
        <TablePagination
          component="div"
          count={data.totalElements}
          page={page}
          rowsPerPage={size}
          rowsPerPageOptions={[10, 20, 50]}
          onPageChange={(_event, newPage) => setPage(newPage)}
          onRowsPerPageChange={(event) => {
            setSize(parseInt(event.target.value, 10));
            setPage(0);
          }}
        />
      </>
    );
  }

  return (
    <>
      <Typography variant="h5" component="h1" gutterBottom>
        Brew Methods
      </Typography>
      {body}
    </>
  );
}
