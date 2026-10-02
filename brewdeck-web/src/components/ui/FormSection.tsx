'use client';

import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import { useId, type ReactNode } from 'react';

/**
 * A titled group of form fields on a 12-column grid. Children are `Grid` items; the section is a
 * region labelled by its heading, so screen readers announce it.
 */
export function FormSection({ title, children }: { title: string; children: ReactNode }) {
  const headingId = useId();
  return (
    <Box component="section" aria-labelledby={headingId}>
      <Typography
        id={headingId}
        variant="overline"
        component="h3"
        color="text.secondary"
        sx={{ display: 'block', fontWeight: 600, mb: 1 }}
      >
        {title}
      </Typography>
      <Grid container spacing={2}>
        {children}
      </Grid>
    </Box>
  );
}
