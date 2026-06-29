import { Inject, Injectable } from '@nestjs/common';
import { CLOCK, type IClock } from '@/shared/application/clock/clock.port';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import { SyncAuthenticatedUserUseCase } from '../../../users/application/use-cases/sync-authenticated-user.use-case';
import {
  assertReservationStartsInFuture,
  assertCourtCanBeReserved,
  resolveReservationTime
} from '../../domain/services/reservation-slot-policy';
import {
  RESERVATION_REPOSITORY,
  type IReservationRepository,
  type IReservationSnapshot
} from '../../domain/repositories/reservation.repository';

export interface ICreateReservationInput {
  courtId: string;
  startsAt: string;
}

export type ICreatedReservationOutput = Omit<IReservationSnapshot, 'userId'>;

@Injectable()
export class CreateReservationUseCase {
  constructor(
    @Inject(RESERVATION_REPOSITORY)
    private readonly reservationRepository: IReservationRepository,
    @Inject(SyncAuthenticatedUserUseCase)
    private readonly syncAuthenticatedUser: SyncAuthenticatedUserUseCase,
    @Inject(CLOCK)
    private readonly clock: IClock
  ) {}

  async execute(
    user: IAuthenticatedUserOutput,
    input: ICreateReservationInput
  ): Promise<ICreatedReservationOutput> {
    const resolved = resolveReservationTime(input.startsAt);
    assertReservationStartsInFuture(resolved, this.clock.now());
    const localUser = await this.syncAuthenticatedUser.execute(user);
    const reservationWindow = await this.reservationRepository.getReservationWindow({
      courtId: input.courtId,
      date: resolved.date
    });

    assertCourtCanBeReserved(reservationWindow, resolved);

    const reservation = await this.reservationRepository.createConfirmedReservation({
      userId: localUser.id,
      courtId: input.courtId,
      startsAt: resolved.startsAt,
      endsAt: resolved.endsAt
    });

    return {
      id: reservation.id,
      courtId: reservation.courtId,
      startsAt: reservation.startsAt,
      endsAt: reservation.endsAt,
      status: reservation.status
    };
  }
}
