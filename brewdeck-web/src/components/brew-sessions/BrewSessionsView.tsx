'use client';

import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import TablePagination from '@mui/material/TablePagination';
import Typography from '@mui/material/Typography';
import { useState, type ReactNode } from 'react';
import { useBrewSessions } from '@/hooks/useBrewSessions';
import { useDebounce } from '@/hooks/useDebounce';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { BrewSessionFilters } from './BrewSessionFilters';
import { BrewSessionsTable } from './BrewSessionsTable';
import { BrewSessionFormDialog } from './BrewSessionFormDialog';
import type { BrewSessionFilters as Filters } from '@/lib/api/types';

export function BrewSessionsView() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [filters, setFilters] = useState<Filters>({});
  const debouncedFilters = useDebounce(filters, 300);

  const { data, isLoading, isError, refetch } = useBrewSessions({
    page,
    size,
    filters: debouncedFilters,
  });

  const handleFiltersChange = (next: Filters) => {
    setPage(0);
    setFilters(next);
  };

  const [createOpen, setCreateOpen] = useState(false);

  let body: ReactNode;
  if (isLoading && !data) {
    body = <Spinner />;
  } else if (isError || !data) {
    body = <ErrorState message="Could not load brew sessions." onRetry={() => refetch()} />;
  } else if (data.content.length === 0) {
    body = <EmptyState message="No brew sessions found." />;
  } else {
    body = (
      <>
        <BrewSessionsTable sessions={data.content} />
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
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
        <Typography variant="h5" component="h1">
          Brew Sessions
        </Typography>
        <Button variant="contained" onClick={() => setCreateOpen(true)}>
          Add Brew Session
        </Button>
      </Box>
      <BrewSessionFilters value={filters} onChange={handleFiltersChange} />
      {body}

      {createOpen ? (
        <BrewSessionFormDialog open onClose={() => setCreateOpen(false)} />
      ) : null}
    </>
  );
}
