/**
 * Default display name used when the reviewer has no stored name.
 */
const ANONYMOUS_DISPLAY_NAME = 'Player';

/**
 * Builds the safe "Diego R." style display name for a reviewer.
 *
 * @param name - Stored user name, possibly null or blank.
 * @returns Reviewer display name safe to return to clients.
 */
export function buildReviewerDisplayName(name: string | null | undefined): string {
  const parts = (name ?? '')
    .trim()
    .split(/\s+/)
    .filter((part) => part.length > 0);

  if (parts.length === 0) {
    return ANONYMOUS_DISPLAY_NAME;
  }

  const [first, ...rest] = parts;
  const lastInitial = rest.length > 0 ? rest[rest.length - 1]!.charAt(0).toUpperCase() : null;

  if (lastInitial == null) {
    return `${first}.`;
  }

  return `${first} ${lastInitial}.`;
}

/**
 * Derives the avatar initials for a reviewer (e.g. "DR" for "Diego R.").
 *
 * @param name - Stored user name, possibly null or blank.
 * @returns Two-letter uppercase initials safe to return to clients.
 */
export function buildReviewerInitials(name: string | null | undefined): string {
  const parts = (name ?? '')
    .trim()
    .split(/\s+/)
    .filter((part) => part.length > 0);

  if (parts.length === 0) {
    return ANONYMOUS_DISPLAY_NAME.charAt(0).toUpperCase().repeat(2);
  }

  const firstInitial = parts[0]!.charAt(0).toUpperCase();
  const lastInitial =
    parts.length > 1 ? parts[parts.length - 1]!.charAt(0).toUpperCase() : '';

  const initials = `${firstInitial}${lastInitial}`;

  if (initials.length >= 2) {
    return initials;
  }

  return `${initials}${initials.charAt(0).toUpperCase()}`;
}
