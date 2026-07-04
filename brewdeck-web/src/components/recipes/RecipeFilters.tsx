'use client';

import Box from '@mui/material/Box';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import TextField from '@mui/material/TextField';
import type { ChangeEvent } from 'react';
import type { RecipeFilters as Filters } from '@/lib/api/types';

export function RecipeFilters({
  value,
  onChange,
}: {
  value: Filters;
  onChange: (next: Filters) => void;
}) {
  const handleName = (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...value, name: event.target.value });
  };

  const handleFavorite = (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...value, favorite: event.target.checked ? true : undefined });
  };

  return (
    <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap', mb: 2 }}>
      <TextField label="Name" size="small" value={value.name ?? ''} onChange={handleName} />
      <FormControlLabel
        control={<Checkbox checked={value.favorite ?? false} onChange={handleFavorite} />}
        label="Favorites only"
      />
    </Box>
  );
}
