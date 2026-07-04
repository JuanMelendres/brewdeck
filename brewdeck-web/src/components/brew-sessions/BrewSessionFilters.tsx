'use client';

import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import type { ChangeEvent } from 'react';
import type { BrewSessionFilters as Filters } from '@/lib/api/types';

export function BrewSessionFilters({
  value,
  onChange,
}: {
  value: Filters;
  onChange: (next: Filters) => void;
}) {
  const handleRating = (event: ChangeEvent<HTMLInputElement>) => {
    const raw = event.target.value;
    onChange({ ...value, rating: raw === '' ? undefined : Number(raw) });
  };

  return (
    <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap', mb: 2 }}>
      <TextField
        label="Rating"
        type="number"
        size="small"
        value={value.rating ?? ''}
        onChange={handleRating}
      />
    </Box>
  );
}
