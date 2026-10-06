import { cookies, headers } from 'next/headers';
import { getRequestConfig } from 'next-intl/server';
import { LOCALE_COOKIE, resolveLocale } from './config';
import { enabledLocales } from './enabledLocales';

// Locale per request: the saved choice (cookie) wins, then the browser language. No locale in the URL.
export default getRequestConfig(async () => {
  const locale = resolveLocale(
    (await cookies()).get(LOCALE_COOKIE)?.value,
    (await headers()).get('accept-language'),
    await enabledLocales(),
  );

  return {
    locale,
    messages: (await import(`../../messages/${locale}.json`)).default,
  };
});
