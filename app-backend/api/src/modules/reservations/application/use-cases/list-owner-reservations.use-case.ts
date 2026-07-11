import { Inject, Injectable } from '@nestjs/common';
import { PinoLogger } from 'nestjs-pino';
import {
  FILE_READ_URL_PORT,
  type IFileReadUrlPort
} from '@/modules/files/application/ports/file-read-url.port';
import { BaseError } from '@/shared/domain/errors/base.error';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import type {
  IOwnerReservationCardOutput,
  IOwnerReservationsOutput
} from '../dto/owner-reservations.output';
import {
  RESERVATION_REPOSITORY,
  type IListOwnerReservationsQuery,
  type IOwnerReservationSnapshot,
  type IReservationRepository
} from '../../domain/repositories/reservation.repository';

export const OWNER_RESERVATIONS_UPCOMING_LIMIT = 20;
export const OWNER_RESERVATIONS_FINALIZED_LIMIT = 20;

/**
 * Request shape accepted by the owner reservations use case.
 */
export interface IListOwnerReservationsRequest {
  courtId?: string;
}

/**
 * Lists reservations booked by other players on the authenticated owner courts,
 * grouped into upcoming and finalized sections and optionally filtered to a
 * single owned court.
 */
@Injectable()
export class ListOwnerReservationsUseCase {
  constructor(
    @Inject(RESERVATION_REPOSITORY)
    private readonly reservationRepository: IReservationRepository,
    @Inject(FILE_READ_URL_PORT)
    private readonly fileReadUrl: IFileReadUrlPort,
    @Inject(PinoLogger)
    private readonly logger: PinoLogger
  ) {}

  async execute(
    user: IAuthenticatedUserOutput,
    request: IListOwnerReservationsRequest
  ): Promise<IOwnerReservationsOutput> {
    const query: IListOwnerReservationsQuery = {
      ownerIdentity: {
        sub: user.sub,
        provider: user.provider
      },
      ...(request.courtId != null ? { court: { courtId: request.courtId } } : {}),
      upcomingLimit: OWNER_RESERVATIONS_UPCOMING_LIMIT,
      finalizedLimit: OWNER_RESERVATIONS_FINALIZED_LIMIT
    };

    const reservations = await this.reservationRepository.listOwnerReservations(query);

    const upcoming = await Promise.all(
      reservations.upcoming.map((reservation) =>
        this.toCard(reservation, 'UPCOMING')
      )
    );

    const finalized = await Promise.all(
      reservations.finalized.map((reservation) =>
        this.toCard(reservation, 'FINALIZED')
      )
    );

    return {
      selectedCourtId: request.courtId ?? null,
      upcoming,
      finalized
    };
  }

  private async toCard(
    reservation: IOwnerReservationSnapshot,
    section: IOwnerReservationCardOutput['section']
  ): Promise<IOwnerReservationCardOutput> {
    return {
      id: reservation.id,
      complexName: reservation.complexName,
      courtName: reservation.courtName,
      imageUrl: await this.tryCreateImageReadUrl(
        reservation.imageObjectKey,
        reservation.id
      ),
      startsAt: reservation.startsAt,
      endsAt: reservation.endsAt,
      status: section === 'UPCOMING' ? 'CONFIRMED' : 'COMPLETED',
      section
    };
  }

  private async tryCreateImageReadUrl(
    objectKey: string | undefined,
    reservationId: string
  ): Promise<string | undefined> {
    if (objectKey == null) {
      return undefined;
    }

    try {
      return await this.fileReadUrl.createReadUrl(objectKey);
    } catch (error) {
      this.logger.warn(
        buildImageSigningWarningPayload(reservationId, error),
        'Unable to create owner reservation card image read URL.'
      );
      return undefined;
    }
  }
}

function buildImageSigningWarningPayload(reservationId: string, error: unknown): {
  reservationId: string;
  errorName?: string;
  errorCode?: string;
} {
  return {
    reservationId,
    errorName: error instanceof Error ? error.name : undefined,
    errorCode: error instanceof BaseError ? error.code : undefined
  };
}
