import { describe, expect, it } from 'vitest';
import { contentSecurityPolicy, securityHeaderRules } from './headers';

function headersFor(source: string, rules = securityHeaderRules({ isDev: false })) {
  return rules.find((rule) => rule.source === source)?.headers ?? [];
}

describe('contentSecurityPolicy', () => {
  it('locks production down to the same origin and forbids framing', () => {
    const csp = contentSecurityPolicy({ isDev: false });
    expect(csp).toContain("default-src 'self'");
    expect(csp).toContain("script-src 'self' 'unsafe-inline'");
    expect(csp).not.toContain('unsafe-eval');
    expect(csp).toContain("connect-src 'self'");
    expect(csp).toContain("frame-ancestors 'none'");
    expect(csp).toContain("object-src 'none'");
    expect(csp).toContain('upgrade-insecure-requests');
  });

  it('allows eval and plain http in development', () => {
    const csp = contentSecurityPolicy({ isDev: true });
    expect(csp).toContain("script-src 'self' 'unsafe-inline' 'unsafe-eval'");
    expect(csp).not.toContain('upgrade-insecure-requests');
  });

  it('adds the API origin to connect-src when the browser calls another origin', () => {
    const csp = contentSecurityPolicy({ isDev: false, apiBaseUrl: 'https://api.example.com/base' });
    expect(csp).toContain("connect-src 'self' https://api.example.com;");
  });

  it('ignores an API base URL that is not a valid URL', () => {
    const csp = contentSecurityPolicy({ isDev: false, apiBaseUrl: 'not a url' });
    expect(csp).toContain("connect-src 'self';");
  });
});

describe('securityHeaderRules', () => {
  it('sends the baseline security headers on every path', () => {
    const keys = headersFor('/:path*').map((header) => header.key);
    expect(keys).toEqual([
      'Content-Security-Policy-Report-Only',
      'X-Frame-Options',
      'X-Content-Type-Options',
      'Referrer-Policy',
      'Permissions-Policy',
    ]);
    expect(headersFor('/:path*')).toContainEqual({ key: 'X-Frame-Options', value: 'DENY' });
  });

  it.each(['/share/:path*', '/reset-password', '/verify-email'])(
    'sends no referrer from %s, whose URL carries a token',
    (source) => {
      expect(headersFor(source)).toEqual([{ key: 'Referrer-Policy', value: 'no-referrer' }]);
    },
  );

  it('lists the token pages after the catch-all so they override it', () => {
    const sources = securityHeaderRules({ isDev: false }).map((rule) => rule.source);
    expect(sources[0]).toBe('/:path*');
    expect(sources.indexOf('/share/:path*')).toBeGreaterThan(0);
  });
});
