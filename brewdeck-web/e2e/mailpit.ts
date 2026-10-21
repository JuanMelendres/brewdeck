import { expect, type APIRequestContext } from '@playwright/test';

/** Mailpit catches the API's emails in local and CI runs (docker-compose.yml, e2e.yml). */
const MAILPIT_URL = process.env.MAILPIT_URL ?? 'http://localhost:8025';

interface MailpitSearch {
  messages: { ID: string }[];
}

interface MailpitMessage {
  Text: string;
}

/**
 * Waits for the newest email to `to` and returns the first link in it that starts with `prefix`.
 * Emails are sent after the API's transaction commits, on a background thread, so they can arrive
 * a moment after the page shows success.
 */
export async function linkFromLatestEmail(
  request: APIRequestContext,
  to: string,
  prefix: string,
): Promise<string> {
  let link: string | undefined;
  await expect
    .poll(
      async () => {
        const search = await request.get(`${MAILPIT_URL}/api/v1/search`, {
          params: { query: `to:"${to}"` },
        });
        const { messages } = (await search.json()) as MailpitSearch;
        if (messages.length === 0) {
          return undefined;
        }
        const message = await request.get(`${MAILPIT_URL}/api/v1/message/${messages[0].ID}`);
        const { Text } = (await message.json()) as MailpitMessage;
        link = Text.split(/\s+/).find((word) => word.startsWith(prefix));
        return link;
      },
      { message: `an email to ${to} with a ${prefix} link`, timeout: 15_000 },
    )
    .toBeDefined();
  return link as string;
}
