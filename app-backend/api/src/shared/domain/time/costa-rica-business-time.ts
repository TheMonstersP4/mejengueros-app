import type { Weekday } from '@/generated/prisma/enums';

const DATE_ONLY_PATTERN = /^\d{4}-\d{2}-\d{2}$/;
const MINUTES_PER_HOUR = 60;
const COSTA_RICA_UTC_OFFSET_MINUTES = -6 * MINUTES_PER_HOUR;
const COSTA_RICA_OFFSET_MILLISECONDS = COSTA_RICA_UTC_OFFSET_MINUTES * 60_000;

const WEEKDAY_BY_INDEX: Weekday[] = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY'
];

export function parseCostaRicaBusinessDate(value: string): Date {
  if (!DATE_ONLY_PATTERN.test(value)) {
    throw new Error(`Invalid Costa Rica business date: ${value}`);
  }

  const [year, month, day] = value.split('-').map(Number);
  const parsed = new Date(Date.UTC(year, month - 1, day, 6, 0, 0, 0));

  if (Number.isNaN(parsed.getTime()) || formatCostaRicaBusinessDate(parsed) !== value) {
    throw new Error(`Invalid Costa Rica business date: ${value}`);
  }

  return parsed;
}

export function formatCostaRicaBusinessDate(value: Date): string {
  const shifted = new Date(value.getTime() + COSTA_RICA_OFFSET_MILLISECONDS);
  const year = shifted.getUTCFullYear().toString().padStart(4, '0');
  const month = String(shifted.getUTCMonth() + 1).padStart(2, '0');
  const day = String(shifted.getUTCDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function isSameCostaRicaBusinessDate(left: Date, right: Date): boolean {
  return formatCostaRicaBusinessDate(left) === formatCostaRicaBusinessDate(right);
}

export function weekdayInCostaRica(value: Date): Weekday {
  const shifted = new Date(value.getTime() + COSTA_RICA_OFFSET_MILLISECONDS);
  return WEEKDAY_BY_INDEX[shifted.getUTCDay()];
}

export function minutesInCostaRica(value: Date): number {
  const shifted = new Date(value.getTime() + COSTA_RICA_OFFSET_MILLISECONDS);
  return shifted.getUTCHours() * MINUTES_PER_HOUR + shifted.getUTCMinutes();
}

export function costaRicaBusinessDateToUtcInstant(date: Date, minutes: number): Date {
  return new Date(
    Date.UTC(
      date.getUTCFullYear(),
      date.getUTCMonth(),
      date.getUTCDate(),
      0,
      minutes - COSTA_RICA_UTC_OFFSET_MINUTES,
      0,
      0
    )
  );
}

export function addCostaRicaBusinessDays(date: Date, days: number): Date {
  const next = new Date(date);
  next.setUTCDate(next.getUTCDate() + days);
  return next;
}

export function costaRicaBusinessDayBounds(date: string): { start: Date; end: Date } {
  const start = parseCostaRicaBusinessDate(date);
  return {
    start,
    end: addCostaRicaBusinessDays(start, 1)
  };
}
