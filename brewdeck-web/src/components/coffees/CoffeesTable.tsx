'use client';

import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import type { Coffee } from '@/lib/api/types';

function orDash(value: string | null): string {
  return value && value.trim() !== '' ? value : '—';
}

export function CoffeesTable({ coffees }: { coffees: Coffee[] }) {
  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Name</TableCell>
            <TableCell>Brand</TableCell>
            <TableCell>Origin</TableCell>
            <TableCell>Roast</TableCell>
            <TableCell>Process</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {coffees.map((coffee) => (
            <TableRow key={coffee.id}>
              <TableCell>{coffee.name}</TableCell>
              <TableCell>{orDash(coffee.brand)}</TableCell>
              <TableCell>{orDash(coffee.origin)}</TableCell>
              <TableCell>{orDash(coffee.roastLevel)}</TableCell>
              <TableCell>{orDash(coffee.process)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
