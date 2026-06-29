import { Inject, Injectable } from '@nestjs/common';
import type { CourtStatus } from '@/generated/prisma/enums';
import { CLOCK, type IClock } from '@/shared/application/clock/clock.port';
import {
  buildReservableSlots,
  parseDateOnly,
  type IReservableSlot,
  type IReservableSlotsAvailabilityStatus
} from '../../domain/services/reservation-slot-policy';
import {
  RESERVATION_REPOSITORY,
  type IReservationRepository
} from '../../domain/repositories/reservation.repository';

export interface IReservableSlotsOutput {
  court: {
    id: string;
    name: string;
    status: CourtStatus;
  };
  date: string;
  availabilityStatus: IReservableSlotsAvailabilityStatus;
  slots: IReservableSlot[];
}

@Injectable()
export class GetReservableSlotsUseCase {
  constructor(
    @Inject(RESERVATION_REPOSITORY)
    private readonly reservationRepository: IReservationRepository,
    @Inject(CLOCK)
    private readonly clock: IClock
  ) {}

  async execute(courtId: string, date: string): Promise<IReservableSlotsOutput> {
    parseDateOnly(date);

    const reservationWindow = await this.reservationRepository.getReservationWindow({
      courtId,
      date
    });
    const reservableSlots = buildReservableSlots(
      reservationWindow,
      date,
      this.clock.now()
    );

    return {
      court: {
        id: reservationWindow.court.id,
        name: reservationWindow.court.name,
        status: reservationWindow.court.status
      },
      date,
      availabilityStatus: reservableSlots.availabilityStatus,
      slots: reservableSlots.slots
    };
  }
}
