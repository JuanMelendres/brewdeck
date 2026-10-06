'use client';

import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import Skeleton from '@mui/material/Skeleton';
import { useTranslations } from 'next-intl';
import { radius } from '@/lib/theme/tokens';

/** Placeholder rows shown while the first page of a table loads. */
export function TableSkeleton({ rows = 6, columns = 4 }: { rows?: number; columns?: number }) {
  const t = useTranslations('common');
  return (
    <Paper variant="outlined" role="status" aria-label={t('loading')} sx={{ borderRadius: radius.card, px: 2.5, py: 1.5 }}>
      {Array.from({ length: rows }, (_, row) => (
        <Box
          key={row}
          sx={{
            display: 'grid',
            gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))`,
            gap: 3,
            py: 1.75,
            borderBottom: row < rows - 1 ? 1 : 0,
            borderColor: 'divider',
          }}
        >
          {Array.from({ length: columns }, (_, column) => (
            <Skeleton key={column} variant="text" width={column === 0 ? '80%' : '55%'} />
          ))}
        </Box>
      ))}
    </Paper>
  );
}
