'use client';

import CoffeeOutlinedIcon from '@mui/icons-material/CoffeeOutlined';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import TablePagination from '@mui/material/TablePagination';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { useTranslations } from 'next-intl';
import { useCoffees } from '@/hooks/useCoffees';
import { useDebounce } from '@/hooks/useDebounce';
import { ErrorState } from '@/components/ui/ErrorState';
import { CardGridSkeleton } from '@/components/ui/CardGrid';
import { EmptyState } from '@/components/ui/EmptyState';
import { CoffeeFilters } from './CoffeeFilters';
import { CoffeeFormDialog } from './CoffeeFormDialog';
import { CoffeeCards } from './CoffeeCards';
import { DeleteCoffeeDialog } from './DeleteCoffeeDialog';
import type { Coffee, CoffeeFilters as Filters } from '@/lib/api/types';

export function CoffeesView() {
  const t = useTranslations('coffees.list');
  const tc = useTranslations('common');
  const [page, setPage] = useState(0);
  // Multiples of 12 fill the 1-, 2-, and 3-column card grids evenly.
  const [size, setSize] = useState(12);
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
    body = <CardGridSkeleton />;
  } else if (isError || !data) {
    body = <ErrorState message={t('loadFailed')} onRetry={() => refetch()} />;
  } else if (data.content.length === 0) {
    const filtered = Object.values(debouncedFilters).some((value) => value !== undefined && value !== '');
    body = filtered ? (
      <EmptyState message={t('noMatches')} />
    ) : (
      <EmptyState
        icon={<CoffeeOutlinedIcon />}
        message={t('empty')}
        action={{ label: t('addFirst'), onClick: () => setCreateOpen(true) }}
      />
    );
  } else {
    body = (
      <>
        <CoffeeCards
          coffees={data.content}
          onEdit={(coffee) => setEditing(coffee)}
          onDelete={(coffee) => setDeleting(coffee)}
        />
        <TablePagination
          component="div"
          count={data.totalElements}
          page={page}
          rowsPerPage={size}
          rowsPerPageOptions={[12, 24, 48]}
          labelRowsPerPage={tc('perPage')}
          sx={{ mt: 1 }}
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
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2, mb: 2 }}>
        <Typography variant="h4" component="h1">
          {t('title')}
        </Typography>
        <Button variant="contained" onClick={() => setCreateOpen(true)}>
          {t('add')}
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
