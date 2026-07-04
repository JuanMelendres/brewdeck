'use client';

import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import type { BrewSession } from '@/lib/api/types';

function orDash(value: string | number | null): string {
  if (value === null) {
    return '—';
  }
  const text = String(value);
  return text.trim() !== '' ? text : '—';
}

function formatDateTime(iso: string): string {
  return iso.replace('T', ' ').slice(0, 16);
}

export function BrewSessionsTable({ sessions }: { sessions: BrewSession[] }) {
  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Recipe</TableCell>
            <TableCell>Brewed At</TableCell>
            <TableCell>Rating</TableCell>
            <TableCell>Actual Temp</TableCell>
            <TableCell>Actual Time</TableCell>
            <TableCell>Taste</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {sessions.map((session) => (
            <TableRow key={session.id}>
              <TableCell>{session.recipeName}</TableCell>
              <TableCell>{formatDateTime(session.brewedAt)}</TableCell>
              <TableCell>{orDash(session.rating)}</TableCell>
              <TableCell>{orDash(session.actualTemp)}</TableCell>
              <TableCell>{orDash(session.actualTime)}</TableCell>
              <TableCell>{orDash(session.tasteResult)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
