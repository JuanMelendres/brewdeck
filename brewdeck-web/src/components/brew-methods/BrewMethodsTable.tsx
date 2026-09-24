'use client';

import Chip from '@mui/material/Chip';
import IconButton from '@mui/material/IconButton';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import type { BrewMethod } from '@/lib/api/brewMethods';

function orDash(value: string | null): string {
  if (value === null) {
    return '—';
  }
  return value.trim() !== '' ? value : '—';
}

export function BrewMethodsTable({
  methods,
  onEdit,
  onDelete,
}: {
  methods: BrewMethod[];
  onEdit?: (method: BrewMethod) => void;
  onDelete?: (method: BrewMethod) => void;
}) {
  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Name</TableCell>
            <TableCell>Description</TableCell>
            <TableCell>Type</TableCell>
            <TableCell align="right">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {methods.map((method) => (
            <TableRow key={method.id}>
              <TableCell>{method.name}</TableCell>
              <TableCell>{orDash(method.description)}</TableCell>
              <TableCell>
                {method.shared ? (
                  <Chip label="Shared" size="small" variant="outlined" />
                ) : (
                  <Chip label="Mine" size="small" color="primary" />
                )}
              </TableCell>
              <TableCell align="right">
                {/* Shared-catalog methods are read-only for regular users; the backend enforces it. */}
                {method.shared ? null : (
                  <>
                    <IconButton
                      aria-label={`edit ${method.name}`}
                      size="small"
                      onClick={() => onEdit?.(method)}
                    >
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <IconButton
                      aria-label={`delete ${method.name}`}
                      size="small"
                      onClick={() => onDelete?.(method)}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
