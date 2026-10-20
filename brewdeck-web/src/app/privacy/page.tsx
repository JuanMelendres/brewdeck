import { getLocale } from 'next-intl/server';
import { LegalDocument } from '@/components/legal/LegalDocument';
import { DEFAULT_LOCALE, isLocale } from '@/i18n/config';
import { readLegalDocument } from '@/lib/legal/readLegalDocument';

export default async function PrivacyPage() {
  const locale = await getLocale();
  const markdown = await readLegalDocument('privacy', isLocale(locale) ? locale : DEFAULT_LOCALE);
  return <LegalDocument markdown={markdown} />;
}
