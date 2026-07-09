import { jsPDF } from 'jspdf';
import type { Recipe } from '@/lib/api/types';

export function orDash(value: string | number | null): string {
  if (value === null) {
    return '—';
  }
  const text = String(value);
  return text.trim() !== '' ? text : '—';
}

export function recipePdfFilename(recipe: Recipe): string {
  const slug = recipe.name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return `${slug || 'recipe'}.pdf`;
}

export function buildRecipePdf(recipe: Recipe): jsPDF {
  const doc = new jsPDF();
  const marginX = 14;
  const maxWidth = doc.internal.pageSize.getWidth() - marginX * 2;
  let y = 20;

  doc.setFontSize(20);
  doc.text('BrewDeck', marginX, y);
  doc.setFontSize(11);
  doc.text('Recipe', marginX, y + 6);
  y += 18;

  doc.setFontSize(16);
  doc.text(recipe.name, marginX, y);
  y += 8;
  if (recipe.favorite) {
    doc.setFontSize(11);
    doc.text('Favorite', marginX, y);
    y += 8;
  }
  y += 6;

  const details: Array<[string, string]> = [
    ['Coffee', recipe.coffeeName],
    ['Method', recipe.methodName],
    ['Coffee (g)', orDash(recipe.coffeeGrams)],
    ['Water (g)', orDash(recipe.waterGrams)],
    ['Ratio', orDash(recipe.ratio)],
    ['Grind', orDash(recipe.grindSetting)],
    ['Water Temp', orDash(recipe.waterTemp)],
    ['Brew Time', orDash(recipe.brewTime)],
  ];
  doc.setFontSize(11);
  for (const [label, value] of details) {
    doc.text(`${label}: ${value}`, marginX, y);
    y += 7;
  }

  if (recipe.steps && recipe.steps.trim() !== '') {
    y += 6;
    doc.setFontSize(13);
    doc.text('Steps', marginX, y);
    y += 7;
    doc.setFontSize(11);
    const lines = doc.splitTextToSize(recipe.steps, maxWidth);
    doc.text(lines, marginX, y);
    y += lines.length * 6;
  }

  if (recipe.expectedTaste && recipe.expectedTaste.trim() !== '') {
    y += 6;
    doc.setFontSize(13);
    doc.text('Expected taste', marginX, y);
    y += 7;
    doc.setFontSize(11);
    const lines = doc.splitTextToSize(recipe.expectedTaste, maxWidth);
    doc.text(lines, marginX, y);
    y += lines.length * 6;
  }

  doc.setFontSize(9);
  doc.text(`Generated ${new Date().toLocaleDateString()}`, marginX, 285);

  return doc;
}

export function downloadRecipePdf(recipe: Recipe): void {
  buildRecipePdf(recipe).save(recipePdfFilename(recipe));
}
