/**
 * Security headers for every response the Next app serves (AUTH-05, finding M2). Spring Security
 * only covers the proxied /api/* JSON; the HTML pages need their own.
 *
 * The CSP ships as Content-Security-Policy-Report-Only first: violations show in the browser
 * console without blocking anything. Switch the header name once staging runs clean. Browsers
 * ignore frame-ancestors and upgrade-insecure-requests in Report-Only, so X-Frame-Options does the
 * real anti-framing work until then.
 *
 * 'unsafe-inline' is needed for MUI's InitColorSchemeScript, Next's inline bootstrap scripts, and
 * Emotion's style tags. Replacing it with a per-request nonce is the next stage.
 *
 * This file is imported by next.config.ts, so it must not use the @/ alias.
 */

export interface HeaderEntry {
  key: string;
  value: string;
}

export interface HeaderRule {
  source: string;
  headers: HeaderEntry[];
}

export interface SecurityHeaderOptions {
  isDev: boolean;
  /** Extra origin the browser calls for the API (NEXT_PUBLIC_API_BASE_URL); empty = same origin. */
  apiBaseUrl?: string;
}

function originOf(url: string | undefined): string | null {
  if (!url) return null;
  try {
    return new URL(url).origin;
  } catch {
    return null;
  }
}

export function contentSecurityPolicy({ isDev, apiBaseUrl }: SecurityHeaderOptions): string {
  const apiOrigin = originOf(apiBaseUrl);
  const directives = [
    "default-src 'self'",
    // Next's dev server (React Refresh) needs eval; production does not.
    `script-src 'self' 'unsafe-inline'${isDev ? " 'unsafe-eval'" : ''}`,
    "style-src 'self' 'unsafe-inline'",
    "img-src 'self' data: blob:",
    // next/font self-hosts the Google fonts at build time.
    "font-src 'self' data:",
    `connect-src 'self'${apiOrigin ? ` ${apiOrigin}` : ''}`,
    "frame-ancestors 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "object-src 'none'",
  ];
  if (!isDev) {
    directives.push('upgrade-insecure-requests');
  }
  return directives.join('; ');
}

// Pages whose URL carries a secret (share token, reset or verification token) must not leak it to
// other sites through the Referer header.
const NO_REFERRER_SOURCES = [
  '/share/:path*',
  '/reset-password',
  '/verify-email',
  '/email-change/:path*',
];

export function securityHeaderRules(options: SecurityHeaderOptions): HeaderRule[] {
  return [
    {
      source: '/:path*',
      headers: [
        { key: 'Content-Security-Policy-Report-Only', value: contentSecurityPolicy(options) },
        { key: 'X-Frame-Options', value: 'DENY' },
        { key: 'X-Content-Type-Options', value: 'nosniff' },
        { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
        { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=()' },
      ],
    },
    // Later rules override earlier ones for the same header key.
    ...NO_REFERRER_SOURCES.map((source) => ({
      source,
      headers: [{ key: 'Referrer-Policy', value: 'no-referrer' }],
    })),
  ];
}
