import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";
import { securityHeaderRules } from "./src/lib/security/headers";

/**
 * The browser only talks to this origin: /api/* is proxied to the Spring Boot API (ADR-013).
 * The refresh cookie is therefore first-party (SameSite=Strict works), and there is no CORS
 * between the browser and the API. API_PROXY_TARGET is read by the Next server, never shipped to
 * the browser.
 */
const apiProxyTarget = process.env.API_PROXY_TARGET ?? "http://localhost:8080";

/**
 * Keeps search engines out unless ALLOW_INDEXING=true (see src/lib/seo/indexing.ts). The header also
 * covers responses without HTML metadata, such as proxied /api/* JSON.
 */
const allowIndexing = process.env.ALLOW_INDEXING === "true";

const nextConfig: NextConfig = {
  experimental: {
    // The proxy's default is 30 s. A local AI model can take longer (ADR-016), and the API waits up
    // to 90 s for it (OLLAMA_TIMEOUT_SECONDS), so the proxy must outlast that.
    proxyTimeout: 100_000,
  },
  async headers() {
    const security = securityHeaderRules({
      isDev: process.env.NODE_ENV === "development",
      apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL,
    });
    return allowIndexing
      ? security
      : [
          { source: "/:path*", headers: [{ key: "X-Robots-Tag", value: "noindex, nofollow" }] },
          ...security,
        ];
  },
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${apiProxyTarget}/api/:path*` }];
  },
};

const withNextIntl = createNextIntlPlugin("./src/i18n/request.ts");

export default withNextIntl(nextConfig);
