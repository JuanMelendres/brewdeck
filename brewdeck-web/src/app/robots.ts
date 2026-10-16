import type { MetadataRoute } from 'next';
import { isIndexingAllowed } from '@/lib/seo/indexing';

export default function robots(): MetadataRoute.Robots {
  return isIndexingAllowed()
    ? { rules: { userAgent: '*', allow: '/', disallow: '/api/' } }
    : { rules: { userAgent: '*', disallow: '/' } };
}
