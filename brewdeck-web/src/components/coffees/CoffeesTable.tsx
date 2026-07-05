'use client';

import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import IconButton from '@mui/material/IconButton';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import type { Coffee } from '@/lib/api/types';

function orDash(value: string | null): string {
  return value && value.trim() !== '' ? value : '—';
}

export function CoffeesTable({
  coffees,
  onEdit,
  onDelete,
}: {
  coffees: Coffee[];
  onEdit?: (coffee: Coffee) => void;
  onDelete?: (coffee: Coffee) => void;
}) {
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
            <TableCell>Actions</TableCell>
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
              <TableCell>
                <IconButton aria-label="edit" size="small" onClick={() => onEdit?.(coffee)}>
                  <EditIcon fontSize="small" />
                </IconButton>
                <IconButton aria-label="delete" size="small" onClick={() => onDelete?.(coffee)}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
