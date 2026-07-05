'use client';

import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import TablePagination from '@mui/material/TablePagination';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { useCoffees } from '@/hooks/useCoffees';
import { useDebounce } from '@/hooks/useDebounce';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import { CoffeeFilters } from './CoffeeFilters';
import { CoffeeFormDialog } from './CoffeeFormDialog';
import { CoffeesTable } from './CoffeesTable';
import { DeleteCoffeeDialog } from './DeleteCoffeeDialog';
import type { Coffee, CoffeeFilters as Filters } from '@/lib/api/types';

export function CoffeesView() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [filters, setFilters] = useState<Filters>({});
  const debouncedFilters = useDebounce(filters, 300);

  const { data, isLoading, isError, refetch } = useCoffees({
    page,
    size,
    filters: debouncedFilters,
  });

  const handleFiltersChange = (next: Filters) => {
    setPage(0);
    setFilters(next);
  };

  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<Coffee | null>(null);
  const [deleting, setDeleting] = useState<Coffee | null>(null);

  let body;
  if (isLoading && !data) {
    body = <Spinner />;
  } else if (isError || !data) {
    body = <ErrorState message="Could not load coffees." onRetry={() => refetch()} />;
  } else if (data.content.length === 0) {
    body = <EmptyState message="No coffees found." />;
  } else {
    body = (
      <>
        <CoffeesTable
          coffees={data.content}
          onEdit={(coffee) => setEditing(coffee)}
          onDelete={(coffee) => setDeleting(coffee)}
        />
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
          Coffees
        </Typography>
        <Button variant="contained" onClick={() => setCreateOpen(true)}>
          Add Coffee
        </Button>
      </Box>
      <CoffeeFilters value={filters} onChange={handleFiltersChange} />
      {body}

      {createOpen ? (
        <CoffeeFormDialog open onClose={() => setCreateOpen(false)} />
      ) : null}
      {editing ? (
        <CoffeeFormDialog open coffee={editing} onClose={() => setEditing(null)} />
      ) : null}
      {deleting ? (
        <DeleteCoffeeDialog open coffee={deleting} onClose={() => setDeleting(null)} />
      ) : null}
    </>
  );
}
