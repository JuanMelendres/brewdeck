'use client';

import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import TablePagination from '@mui/material/TablePagination';
import Typography from '@mui/material/Typography';
import { useState, type ReactNode } from 'react';
import { useBrewMethods } from '@/hooks/useBrewMethods';
import { Spinner } from '@/components/ui/Spinner';
import { ErrorState } from '@/components/ui/ErrorState';
import { EmptyState } from '@/components/ui/EmptyState';
import type { BrewMethod } from '@/lib/api/brewMethods';
import { BrewMethodFormDialog } from './BrewMethodFormDialog';
import { BrewMethodsTable } from './BrewMethodsTable';
import { DeleteBrewMethodDialog } from './DeleteBrewMethodDialog';

export function BrewMethodsView() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const { data, isLoading, isError, refetch } = useBrewMethods({ page, size });

  const [createOpen, setCreateOpen] = useState(false);
  const [editing, setEditing] = useState<BrewMethod | null>(null);
  const [deleting, setDeleting] = useState<BrewMethod | null>(null);

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
        <BrewMethodsTable
          methods={data.content}
          onEdit={(method) => setEditing(method)}
          onDelete={(method) => setDeleting(method)}
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
          Brew Methods
        </Typography>
        <Button variant="contained" onClick={() => setCreateOpen(true)}>
          Add Method
        </Button>
      </Box>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Shared methods are available to everyone. Methods you add are private to you.
      </Typography>
      {body}

      {createOpen ? <BrewMethodFormDialog open onClose={() => setCreateOpen(false)} /> : null}
      {editing ? (
        <BrewMethodFormDialog open method={editing} onClose={() => setEditing(null)} />
      ) : null}
      {deleting ? (
        <DeleteBrewMethodDialog open method={deleting} onClose={() => setDeleting(null)} />
      ) : null}
    </>
  );
}
