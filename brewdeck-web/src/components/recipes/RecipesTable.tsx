'use client';

import IconButton from '@mui/material/IconButton';
import Link from '@mui/material/Link';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import NextLink from 'next/link';
import type { Recipe } from '@/lib/api/types';

function orDash(value: string | number | null): string {
  if (value === null) {
    return '—';
  }
  const text = String(value);
  return text.trim() !== '' ? text : '—';
}

export function RecipesTable({
  recipes,
  onEdit,
  onDelete,
}: {
  recipes: Recipe[];
  onEdit?: (recipe: Recipe) => void;
  onDelete?: (recipe: Recipe) => void;
}) {
  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Name</TableCell>
            <TableCell>Coffee</TableCell>
            <TableCell>Method</TableCell>
            <TableCell>Ratio</TableCell>
            <TableCell>Water Temp</TableCell>
            <TableCell>Favorite</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {recipes.map((recipe) => (
            <TableRow key={recipe.id}>
              <TableCell>
                <Link component={NextLink} href={`/recipes/${recipe.id}`}>
                  {recipe.name}
                </Link>
              </TableCell>
              <TableCell>{recipe.coffeeName}</TableCell>
              <TableCell>{recipe.methodName}</TableCell>
              <TableCell>{orDash(recipe.ratio)}</TableCell>
              <TableCell>{orDash(recipe.waterTemp)}</TableCell>
              <TableCell>{recipe.favorite ? '★' : '—'}</TableCell>
              <TableCell>
                <IconButton aria-label="edit" size="small" onClick={() => onEdit?.(recipe)}>
                  <EditIcon fontSize="small" />
                </IconButton>
                <IconButton aria-label="delete" size="small" onClick={() => onDelete?.(recipe)}>
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
