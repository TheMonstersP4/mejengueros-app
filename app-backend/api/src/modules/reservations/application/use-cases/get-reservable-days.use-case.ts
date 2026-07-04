import { Inject, Injectable } from '@nestjs/common';
import type { CourtStatus } from '@/generated/prisma/enums';
import { CLOCK, type IClock } from '@/shared/application/clock/clock.port';
import { buildReservableSlots, parseDateOnly } from '../../domain/services/reservation-slot-policy';
import {
  RESERVATION_REPOSITORY,
  type IReservationRepository,
  type IReservationWindowSnapshot
} from '../../domain/repositories/reservation.repository';

export interface IReservableDayOutput {
  date: string;
  availabilityStatus: 'AVAILABLE';
  availableSlotsCount: number;
}

export interface IReservableDaysOutput {
  court: {
    id: string;
    name: string;
    status: CourtStatus;
  };
  from: string;
  days: number;
  reservableDays: IReservableDayOutput[];
}

@Injectable()
export class GetReservableDaysUseCase {
  constructor(
    @Inject(RESERVATION_REPOSITORY)
    private readonly reservationRepository: IReservationRepository,
    @Inject(CLOCK)
    private readonly clock: IClock
  ) {}

  async execute(courtId: string, from: string, days: number): Promise<IReservableDaysOutput> {
    const fromDate = parseDateOnly(from);
    const now = this.clock.now();
    let court: IReservationWindowSnapshot['court'] | null = null;
    const reservableDays: IReservableDayOutput[] = [];

    for (let offset = 0; offset < days; offset += 1) {
      const currentDate = addUtcDays(fromDate, offset);
      const currentDateString = currentDate.toISOString().slice(0, 10);
      const reservationWindow = await this.reservationRepository.getReservationWindow({
        courtId,
        date: currentDateString
      });

      court ??= reservationWindow.court;

      const reservableSlots = buildReservableSlots(reservationWindow, currentDateString, now);

      if (reservableSlots.availabilityStatus !== 'AVAILABLE') {
        continue;
      }

      reservableDays.push({
        date: currentDateString,
        availabilityStatus: reservableSlots.availabilityStatus,
        availableSlotsCount: reservableSlots.slots.length
      });
    }

    if (court == null) {
      throw new Error('Reservable day scan requires at least one day.');
    }

    return {
      court: {
        id: court.id,
        name: court.name,
        status: court.status
      },
      from,
      days,
      reservableDays
    };
  }
}

function addUtcDays(date: Date, days: number): Date {
  const next = new Date(date);
  next.setUTCDate(next.getUTCDate() + days);
  return next;
}
