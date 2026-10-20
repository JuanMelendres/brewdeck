import { readFile } from 'node:fs/promises';
import path from 'node:path';
import type { AppLocale } from '@/i18n/config';

export type LegalDocumentKind = 'privacy' | 'terms';

/**
 * Reads a legal text from `content/legal/` on the server (LEGAL-01). These Markdown files are the
 * single source of the texts; `next.config.ts` traces them into the server bundle.
 */
export async function readLegalDocument(kind: LegalDocumentKind, locale: AppLocale): Promise<string> {
  const file = path.join(process.cwd(), 'content', 'legal', `${kind}.${locale}.md`);
  return readFile(file, 'utf8');
}
