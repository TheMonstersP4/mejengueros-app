import type { CourtStatus, Weekday } from '@/generated/prisma/enums';
import { CourtNotReservableError } from '../errors/court-not-reservable.error';
import { InvalidReservationRequestError } from '../errors/invalid-reservation-request.error';
import { ReservationConflictError } from '../errors/reservation-conflict.error';
import type { IReservationWindowSnapshot } from '../repositories/reservation.repository';
import { parseUtcReservationStartsAt } from '../../shared/utc-reservation-starts-at';

const DATE_ONLY_PATTERN = /^\d{4}-\d{2}-\d{2}$/;
const MINUTES_PER_HOUR = 60;

export type IReservableSlotsAvailabilityStatus =
  | 'AVAILABLE'
  | 'FULLY_BOOKED'
  | 'UNAVAILABLE';

export interface IResolvedReservationTime {
  startsAt: Date;
  endsAt: Date;
  date: string;
}

export interface IReservableSlot {
  startsAt: string;
  endsAt: string;
}

export interface IReservableSlotsResult {
  availabilityStatus: IReservableSlotsAvailabilityStatus;
  slots: IReservableSlot[];
}

export function resolveReservationTime(startsAt: string): IResolvedReservationTime {
  const parsed = parseUtcReservationStartsAt(startsAt);

  if (parsed == null) {
    throw InvalidReservationRequestError.invalidStartsAt(startsAt);
  }

  return {
    startsAt: parsed,
    endsAt: new Date(parsed.getTime() + MINUTES_PER_HOUR * 60_000),
    date: parsed.toISOString().slice(0, 10)
  };
}

export function assertReservationStartsInFuture(
  resolved: IResolvedReservationTime,
  now: Date
): void {
  if (resolved.startsAt.getTime() <= now.getTime()) {
    throw InvalidReservationRequestError.startedAtOrBeforeNow(
      resolved.startsAt.toISOString(),
      now.toISOString()
    );
  }
}

export function assertCourtCanBeReserved(
  window: IReservationWindowSnapshot,
  resolved: IResolvedReservationTime
): void {
  if (!isCourtActive(window.court.status) || !isCourtActive(window.court.complexStatus)) {
    throw CourtNotReservableError.inactive(window.court.id);
  }

  if (window.availability == null) {
    throw CourtNotReservableError.missingAvailability(window.court.id);
  }

  const weekday = weekdayFromDate(resolved.startsAt);

  if (!window.availability.days.includes(weekday)) {
    throw CourtNotReservableError.unavailableDay(resolved.date, weekday);
  }

  const startMinutes = minutesFromUtcDate(resolved.startsAt);
  const endMinutes = minutesFromUtcDate(resolved.endsAt);
  const availabilityStart = minutesFromTimeOnly(window.availability.startTime);
  const availabilityEnd = minutesFromTimeOnly(window.availability.endTime);

  if (startMinutes < availabilityStart || endMinutes > availabilityEnd) {
    throw CourtNotReservableError.outsideAvailability(
      resolved.startsAt.toISOString(),
      window.availability.startTime,
      window.availability.endTime
    );
  }

  if (window.confirmedStartsAt.includes(resolved.startsAt.toISOString())) {
    throw new ReservationConflictError(window.court.id, resolved.startsAt.toISOString());
  }
}

export function buildReservableSlots(
  window: IReservationWindowSnapshot,
  date: string,
  now: Date = new Date()
): IReservableSlotsResult {
  const day = parseDateOnly(date);

  if (!isCourtActive(window.court.status) || !isCourtActive(window.court.complexStatus)) {
    return { availabilityStatus: 'UNAVAILABLE', slots: [] };
  }

  if (window.availability == null) {
    return { availabilityStatus: 'UNAVAILABLE', slots: [] };
  }

  const weekday = weekdayFromDate(day);

  if (!window.availability.days.includes(weekday)) {
    return { availabilityStatus: 'UNAVAILABLE', slots: [] };
  }

  const availabilityStart = minutesFromTimeOnly(window.availability.startTime);
  const availabilityEnd = minutesFromTimeOnly(window.availability.endTime);
  const slots: IReservableSlot[] = [];
  const confirmedStartsAt = new Set(window.confirmedStartsAt);
  const shouldExcludeStartedSlots = isSameUtcDate(day, now);

  for (
    let startMinutes = availabilityStart;
    startMinutes + MINUTES_PER_HOUR <= availabilityEnd;
    startMinutes += MINUTES_PER_HOUR
  ) {
    const startsAt = utcDateFromMinutes(day, startMinutes);
    const endsAt = utcDateFromMinutes(day, startMinutes + MINUTES_PER_HOUR);

    if (shouldExcludeStartedSlots && startsAt.getTime() <= now.getTime()) {
      continue;
    }

    if (!confirmedStartsAt.has(startsAt.toISOString())) {
      slots.push({
        startsAt: startsAt.toISOString(),
        endsAt: endsAt.toISOString()
      });
    }
  }

  return {
    availabilityStatus: slots.length === 0 ? 'FULLY_BOOKED' : 'AVAILABLE',
    slots
  };
}

export function parseDateOnly(date: string): Date {
  if (!DATE_ONLY_PATTERN.test(date)) {
    throw InvalidReservationRequestError.invalidDate(date);
  }

  const parsed = new Date(`${date}T00:00:00.000Z`);

  if (Number.isNaN(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== date) {
    throw InvalidReservationRequestError.invalidDate(date);
  }

  return parsed;
}

function weekdayFromDate(date: Date): Weekday {
  const weekdayByIndex: Weekday[] = [
    'SUNDAY',
    'MONDAY',
    'TUESDAY',
    'WEDNESDAY',
    'THURSDAY',
    'FRIDAY',
    'SATURDAY'
  ];

  return weekdayByIndex[date.getUTCDay()];
}

function minutesFromTimeOnly(value: string): number {
  const [hours, minutes] = value.split(':').map(Number);
  return hours * MINUTES_PER_HOUR + minutes;
}

function minutesFromUtcDate(value: Date): number {
  return value.getUTCHours() * MINUTES_PER_HOUR + value.getUTCMinutes();
}

function utcDateFromMinutes(day: Date, minutes: number): Date {
  return new Date(
    Date.UTC(
      day.getUTCFullYear(),
      day.getUTCMonth(),
      day.getUTCDate(),
      Math.floor(minutes / MINUTES_PER_HOUR),
      minutes % MINUTES_PER_HOUR,
      0,
      0
    )
  );
}

function isSameUtcDate(left: Date, right: Date): boolean {
  return (
    left.getUTCFullYear() === right.getUTCFullYear() &&
    left.getUTCMonth() === right.getUTCMonth() &&
    left.getUTCDate() === right.getUTCDate()
  );
}

function isCourtActive(status: CourtStatus | string): boolean {
  return status === 'ACTIVE';
}
