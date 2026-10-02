/**
 * Tiny pub/sub so the API client can tell the UI "the server says this account must verify its
 * email" (403 with code EMAIL_NOT_VERIFIED) without importing React. The flag set is cached for the
 * session, so this also covers the flag being switched on mid-session: the backend's answer wins.
 */
type Listener = () => void;

const listeners = new Set<Listener>();

export const EMAIL_NOT_VERIFIED = 'EMAIL_NOT_VERIFIED';

export function onEmailNotVerified(listener: Listener): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function notifyEmailNotVerified(): void {
  listeners.forEach((listener) => listener());
}
