import type en from '../../messages/en.json';
import type { AppLocale } from './config';

// Typed keys: every `t('…')` call is checked against the English messages.
declare module 'next-intl' {
  interface AppConfig {
    Locale: AppLocale;
    Messages: typeof en;
  }
}
