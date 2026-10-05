'use client';

import StarRoundedIcon from '@mui/icons-material/StarRounded';
import Box from '@mui/material/Box';
import Link from '@mui/material/Link';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import NextLink from 'next/link';
import type { BrewSession } from '@/lib/api/types';
import { formatDateTime } from '@/lib/format/dates';
import { radius } from '@/lib/theme/tokens';

function Muted({ children }: { children: string }) {
  return (
    <Typography component="span" variant="body2" color="text.secondary">
      {children}
    </Typography>
  );
}

function orMuted(value: string | number | null) {
  return value === null || value === '' ? <Muted>—</Muted> : value;
}

export function BrewSessionsTable({ sessions }: { sessions: BrewSession[] }) {
  return (
    <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: radius.card }}>
      <Table sx={{ minWidth: 720, '& td, & th': { px: 2.5 }, '& td': { py: 1.75 } }}>
        <TableHead>
          <TableRow>
            <TableCell>Brewed</TableCell>
            <TableCell>Recipe</TableCell>
            <TableCell>Rating</TableCell>
            <TableCell>Temp</TableCell>
            <TableCell>Time</TableCell>
            <TableCell>Taste</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {sessions.map((session) => (
            <TableRow key={session.id} hover>
              <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatDateTime(session.brewedAt)}</TableCell>
              <TableCell>
                <Link
                  component={NextLink}
                  href={`/recipes/${session.recipeId}`}
                  underline="hover"
                  color="text.primary"
                  sx={{ fontWeight: 600 }}
                >
                  {session.recipeName}
                </Link>
              </TableCell>
              <TableCell>
                {session.rating === null ? (
                  <Muted>—</Muted>
                ) : (
                  <Box
                    component="span"
                    sx={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 0.5,
                      px: 1.25,
                      py: 0.25,
                      borderRadius: radius.pill,
                      bgcolor: 'background.tint',
                      color: 'secondary.main',
                      fontWeight: 600,
                    }}
                  >
                    <StarRoundedIcon sx={{ fontSize: 15 }} aria-hidden />
                    <span>{session.rating}</span>
                    <Typography component="span" variant="caption" color="text.secondary">
                      /10
                    </Typography>
                  </Box>
                )}
              </TableCell>
              <TableCell>{session.actualTemp === null ? <Muted>—</Muted> : `${session.actualTemp}°C`}</TableCell>
              <TableCell>{orMuted(session.actualTime)}</TableCell>
              <TableCell sx={{ maxWidth: 280 }}>
                {session.tasteResult ? (
                  <Typography variant="body2" noWrap title={session.tasteResult}>
                    {session.tasteResult}
                  </Typography>
                ) : (
                  <Muted>—</Muted>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
