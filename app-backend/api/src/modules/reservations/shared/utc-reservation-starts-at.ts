export const UTC_RESERVATION_STARTS_AT_SCHEMA_PATTERN =
  '^\\d{4}-\\d{2}-\\d{2}T\\d{2}:00:00(?:\\.000)?Z$';

export const UTC_RESERVATION_STARTS_AT_PATTERN = new RegExp(
  UTC_RESERVATION_STARTS_AT_SCHEMA_PATTERN
);

export const UTC_RESERVATION_STARTS_AT_MESSAGE =
  'Reservation start time must be a real UTC ISO datetime with explicit Z aligned to a whole hour.';

export function parseUtcReservationStartsAt(value: string): Date | null {
  if (!UTC_RESERVATION_STARTS_AT_PATTERN.test(value)) {
    return null;
  }

  const parsed = new Date(value);

  if (Number.isNaN(parsed.getTime())) {
    return null;
  }

  const canonicalWithMilliseconds = parsed.toISOString();
  const canonicalWithoutMilliseconds = canonicalWithMilliseconds.replace('.000Z', 'Z');

  if (value !== canonicalWithMilliseconds && value !== canonicalWithoutMilliseconds) {
    return null;
  }

  return parsed;
}
