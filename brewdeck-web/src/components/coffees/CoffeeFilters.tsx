'use client';

import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import type { ChangeEvent } from 'react';
import type { CoffeeFilters as Filters } from '@/lib/api/types';

export function CoffeeFilters({
  value,
  onChange,
}: {
  value: Filters;
  onChange: (next: Filters) => void;
}) {
  const handle = (key: keyof Filters) => (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...value, [key]: event.target.value });
  };

  return (
    <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 2 }}>
      <TextField label="Name" size="small" value={value.name ?? ''} onChange={handle('name')} />
      <TextField label="Origin" size="small" value={value.origin ?? ''} onChange={handle('origin')} />
      <TextField
        label="Roast Level"
        size="small"
        value={value.roastLevel ?? ''}
        onChange={handle('roastLevel')}
      />
      <TextField label="Process" size="small" value={value.process ?? ''} onChange={handle('process')} />
    </Box>
  );
}
