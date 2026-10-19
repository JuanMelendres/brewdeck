/**
 * Whether search engines may index this deployment. Off unless ALLOW_INDEXING=true (server-side),
 * so local, staging, and any new environment stay out of search results by default. Only polite
 * crawlers honor it; abusive bots are the edge firewall's job.
 */
export function isIndexingAllowed(env: Record<string, string | undefined> = process.env): boolean {
  return env.ALLOW_INDEXING === 'true';
}
