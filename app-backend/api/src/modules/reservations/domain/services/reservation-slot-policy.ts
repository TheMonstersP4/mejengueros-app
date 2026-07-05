import type { CourtStatus, Weekday } from '@/generated/prisma/enums';
import {
  costaRicaBusinessDateToUtcInstant,
  formatCostaRicaBusinessDate,
  isSameCostaRicaBusinessDate,
  minutesInCostaRica,
  parseCostaRicaBusinessDate,
  weekdayInCostaRica
} from '@/shared/domain/time/costa-rica-business-time';
import { CourtNotReservableError } from '../errors/court-not-reservable.error';
import { InvalidReservationRequestError } from '../errors/invalid-reservation-request.error';
import { ReservationConflictError } from '../errors/reservation-conflict.error';
import type { IReservationWindowSnapshot } from '../repositories/reservation.repository';
import { parseUtcReservationStartsAt } from '../../shared/utc-reservation-starts-at';

const DATE_ONLY_PATTERN = /^\d{4}-\d{2}-\d{2}$/;
const MINUTES_PER_HOUR = 60;
export const SAME_DAY_RESERVATION_MINIMUM_ADVANCE_MINUTES = 30;

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
    date: formatCostaRicaBusinessDate(parsed)
  };
}

export function assertReservationStartsWithMinimumAdvance(
  resolved: IResolvedReservationTime,
  now: Date
): void {
  if (resolved.startsAt.getTime() <= now.getTime()) {
    throw InvalidReservationRequestError.startedAtOrBeforeNow(
      resolved.startsAt.toISOString(),
      now.toISOString()
    );
  }

  if (!isSameCostaRicaBusinessDate(resolved.startsAt, now)) {
    return;
  }

  const minimumReservableStartsAt = new Date(
    now.getTime() + SAME_DAY_RESERVATION_MINIMUM_ADVANCE_MINUTES * 60_000
  );

  if (resolved.startsAt.getTime() <= minimumReservableStartsAt.getTime()) {
    throw InvalidReservationRequestError.sameDayAdvanceThresholdNotMet(
      resolved.startsAt.toISOString(),
      now.toISOString(),
      SAME_DAY_RESERVATION_MINIMUM_ADVANCE_MINUTES
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

  const startMinutes = minutesFromDate(resolved.startsAt);
  const endMinutes = minutesFromDate(resolved.endsAt);
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
  const shouldApplySameDayAdvanceThreshold = isSameCostaRicaBusinessDate(day, now);
  const minimumReservableStartsAt = new Date(
    now.getTime() + SAME_DAY_RESERVATION_MINIMUM_ADVANCE_MINUTES * 60_000
  );

  for (
    let startMinutes = availabilityStart;
    startMinutes + MINUTES_PER_HOUR <= availabilityEnd;
    startMinutes += MINUTES_PER_HOUR
  ) {
    const startsAt = utcDateFromMinutes(day, startMinutes);
    const endsAt = utcDateFromMinutes(day, startMinutes + MINUTES_PER_HOUR);

    if (
      shouldApplySameDayAdvanceThreshold &&
      startsAt.getTime() <= minimumReservableStartsAt.getTime()
    ) {
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
  let parsed: Date;

  if (!DATE_ONLY_PATTERN.test(date)) {
    throw InvalidReservationRequestError.invalidDate(date);
  }

  try {
    parsed = parseCostaRicaBusinessDate(date);
  } catch {
    throw InvalidReservationRequestError.invalidDate(date);
  }

  if (Number.isNaN(parsed.getTime()) || formatCostaRicaBusinessDate(parsed) !== date) {
    throw InvalidReservationRequestError.invalidDate(date);
  }

  return parsed;
}

function weekdayFromDate(date: Date): Weekday {
  return weekdayInCostaRica(date);
}

function minutesFromTimeOnly(value: string): number {
  const [hours, minutes] = value.split(':').map(Number);
  return hours * MINUTES_PER_HOUR + minutes;
}

function minutesFromDate(value: Date): number {
  return minutesInCostaRica(value);
}

function utcDateFromMinutes(day: Date, minutes: number): Date {
  return costaRicaBusinessDateToUtcInstant(day, minutes);
}

function isCourtActive(status: CourtStatus | string): boolean {
  return status === 'ACTIVE';
}
