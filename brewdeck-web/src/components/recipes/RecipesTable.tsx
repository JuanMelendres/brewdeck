'use client';

import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import type { Recipe } from '@/lib/api/types';

function orDash(value: string | number | null): string {
  if (value === null) {
    return '—';
  }
  const text = String(value);
  return text.trim() !== '' ? text : '—';
}

export function RecipesTable({ recipes }: { recipes: Recipe[] }) {
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
          </TableRow>
        </TableHead>
        <TableBody>
          {recipes.map((recipe) => (
            <TableRow key={recipe.id}>
              <TableCell>{recipe.name}</TableCell>
              <TableCell>{recipe.coffeeName}</TableCell>
              <TableCell>{recipe.methodName}</TableCell>
              <TableCell>{orDash(recipe.ratio)}</TableCell>
              <TableCell>{orDash(recipe.waterTemp)}</TableCell>
              <TableCell>{recipe.favorite ? '★' : '—'}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
