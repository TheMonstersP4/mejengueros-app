import { Inject, Injectable } from '@nestjs/common';
import { CLOCK, type IClock } from '@/shared/application/clock/clock.port';
import {
  RESERVATION_REPOSITORY,
  type IReservationRepository
} from '../../domain/repositories/reservation.repository';

@Injectable()
export class CompleteExpiredReservationsUseCase {
  constructor(
    @Inject(RESERVATION_REPOSITORY)
    private readonly reservationRepository: IReservationRepository,
    @Inject(CLOCK)
    private readonly clock: IClock
  ) {}

  async execute(): Promise<number> {
    return this.reservationRepository.completeExpiredReservations({
      now: this.clock.now()
    });
  }
}
