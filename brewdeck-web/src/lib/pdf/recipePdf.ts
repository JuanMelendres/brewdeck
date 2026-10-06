import { jsPDF } from 'jspdf';
import { createTranslator } from 'next-intl';
import en from '../../../messages/en.json';
import es from '../../../messages/es.json';
import { DEFAULT_LOCALE, type AppLocale } from '@/i18n/config';
import type { Recipe } from '@/lib/api/types';
import { formatDate } from '@/lib/format/dates';

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

const MESSAGES = { en, es } as const;

export function buildRecipePdf(recipe: Recipe, locale: AppLocale = DEFAULT_LOCALE): jsPDF {
  const t = createTranslator({ locale, messages: MESSAGES[locale], namespace: 'recipes.pdf' });
  const doc = new jsPDF();
  const marginX = 14;
  const maxWidth = doc.internal.pageSize.getWidth() - marginX * 2;
  let y = 20;

  doc.setFontSize(20);
  doc.text('BrewDeck', marginX, y);
  doc.setFontSize(11);
  doc.text(t('subtitle'), marginX, y + 6);
  y += 18;

  doc.setFontSize(16);
  doc.text(recipe.name, marginX, y);
  y += 8;
  if (recipe.favorite) {
    doc.setFontSize(11);
    doc.text(t('favorite'), marginX, y);
    y += 8;
  }
  y += 6;

  const details: Array<[string, string]> = [
    [t('coffee'), recipe.coffeeName],
    [t('method'), recipe.methodName],
    [t('coffeeGrams'), orDash(recipe.coffeeGrams)],
    [t('waterGrams'), orDash(recipe.waterGrams)],
    [t('ratio'), orDash(recipe.ratio)],
    [t('grind'), orDash(recipe.grindSetting)],
    [t('waterTemp'), orDash(recipe.waterTemp)],
    [t('brewTime'), orDash(recipe.brewTime)],
  ];
  doc.setFontSize(11);
  for (const [label, value] of details) {
    doc.text(`${label}: ${value}`, marginX, y);
    y += 7;
  }

  if (recipe.steps && recipe.steps.trim() !== '') {
    y += 6;
    doc.setFontSize(13);
    doc.text(t('steps'), marginX, y);
    y += 7;
    doc.setFontSize(11);
    const lines = doc.splitTextToSize(recipe.steps, maxWidth);
    doc.text(lines, marginX, y);
    y += lines.length * 6;
  }

  if (recipe.expectedTaste && recipe.expectedTaste.trim() !== '') {
    y += 6;
    doc.setFontSize(13);
    doc.text(t('expectedTaste'), marginX, y);
    y += 7;
    doc.setFontSize(11);
    const lines = doc.splitTextToSize(recipe.expectedTaste, maxWidth);
    doc.text(lines, marginX, y);
    y += lines.length * 6;
  }

  doc.setFontSize(9);
  doc.text(t('generated', { date: formatDate(new Date().toISOString(), undefined, locale) }), marginX, 285);

  return doc;
}

export function downloadRecipePdf(recipe: Recipe, locale: AppLocale = DEFAULT_LOCALE): void {
  buildRecipePdf(recipe, locale).save(recipePdfFilename(recipe));
}
