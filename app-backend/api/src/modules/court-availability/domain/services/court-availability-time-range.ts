import type { Weekday } from '@/generated/prisma/enums';
import { InvalidCourtAvailabilityError } from '../errors/invalid-court-availability.error';
import type { ICourtAvailabilityInput } from '../repositories/court-availability.repository';

const WHOLE_HOUR_PATTERN = /^(?:[01]\d|2[0-3]):00$/;
const MINUTES_PER_HOUR = 60;

export interface IValidatedCourtAvailabilityInput {
  days: Weekday[];
  startTime: string;
  endTime: string;
  startDate: Date;
  endDate: Date;
}

export function validateCourtAvailabilityInput(
  input: ICourtAvailabilityInput
): IValidatedCourtAvailabilityInput {
  const days = Array.from(new Set(input.days));

  if (days.length === 0) {
    throw InvalidCourtAvailabilityError.missingWeekdays();
  }

  if (!WHOLE_HOUR_PATTERN.test(input.startTime) || !WHOLE_HOUR_PATTERN.test(input.endTime)) {
    throw InvalidCourtAvailabilityError.invalidRange(input.startTime, input.endTime);
  }

  const startMinutes = toMinutes(input.startTime);
  const endMinutes = toMinutes(input.endTime);
  const durationMinutes = endMinutes - startMinutes;

  if (durationMinutes < MINUTES_PER_HOUR || durationMinutes % MINUTES_PER_HOUR !== 0) {
    throw InvalidCourtAvailabilityError.invalidRange(input.startTime, input.endTime);
  }

  return {
    days,
    startTime: input.startTime,
    endTime: input.endTime,
    startDate: toTimeOnlyDate(input.startTime),
    endDate: toTimeOnlyDate(input.endTime)
  };
}

export function formatTimeOnly(value: Date): string {
  const hours = String(value.getUTCHours()).padStart(2, '0');
  const minutes = String(value.getUTCMinutes()).padStart(2, '0');

  return `${hours}:${minutes}`;
}

function toMinutes(value: string): number {
  const [hours, minutes] = value.split(':').map(Number);
  return hours * MINUTES_PER_HOUR + minutes;
}

function toTimeOnlyDate(value: string): Date {
  const [hours, minutes] = value.split(':').map(Number);
  return new Date(Date.UTC(1970, 0, 1, hours, minutes, 0, 0));
}
