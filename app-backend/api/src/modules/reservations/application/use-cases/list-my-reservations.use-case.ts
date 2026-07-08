import { Inject, Injectable } from '@nestjs/common';
import { PinoLogger } from 'nestjs-pino';
import {
  FILE_READ_URL_PORT,
  type IFileReadUrlPort
} from '@/modules/files/application/ports/file-read-url.port';
import { BaseError } from '@/shared/domain/errors/base.error';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import { SyncAuthenticatedUserUseCase } from '../../../users/application/use-cases/sync-authenticated-user.use-case';
import type {
  IMyReservationsOutput,
  IReservationCardOutput
} from '../dto/my-reservations.output';
import {
  RESERVATION_REPOSITORY,
  type IMyReservationSnapshot,
  type IReservationRepository
} from '../../domain/repositories/reservation.repository';

const LEAVE_REVIEW_LABEL = 'Dejar reseña';
const ALREADY_REVIEWED_LABEL = 'Ya dejaste tu reseña';
export const MY_RESERVATIONS_UPCOMING_LIMIT = 20;
export const MY_RESERVATIONS_FINALIZED_LIMIT = 20;

/**
 * Lists authenticated-player reservations with backend-calculated review state.
 */
@Injectable()
export class ListMyReservationsUseCase {
  constructor(
    @Inject(RESERVATION_REPOSITORY)
    private readonly reservationRepository: IReservationRepository,
    @Inject(SyncAuthenticatedUserUseCase)
    private readonly syncAuthenticatedUser: SyncAuthenticatedUserUseCase,
    @Inject(FILE_READ_URL_PORT)
    private readonly fileReadUrl: IFileReadUrlPort,
    @Inject(PinoLogger)
    private readonly logger: PinoLogger
  ) {}

  /**
   * Synchronizes the authenticated user locally and returns grouped reservation cards.
   */
  async execute(user: IAuthenticatedUserOutput): Promise<IMyReservationsOutput> {
    const localUser = await this.syncAuthenticatedUser.execute(user);
    const reservations = await this.reservationRepository.findMyReservationsByUserId({
      userId: localUser.id,
      upcomingLimit: MY_RESERVATIONS_UPCOMING_LIMIT,
      finalizedLimit: MY_RESERVATIONS_FINALIZED_LIMIT
    });

    const upcoming = await Promise.all(
      reservations.upcoming.map((reservation) => this.toUpcomingCard(reservation))
    );

    const finalized = await Promise.all(
      reservations.finalized.map((reservation) => this.toFinalizedCard(reservation))
    );

    return { upcoming, finalized };
  }

  private async toUpcomingCard(
    reservation: IMyReservationSnapshot
  ): Promise<IReservationCardOutput> {
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
      status: 'CONFIRMED',
      section: 'UPCOMING',
      reviewStatus: 'NOT_APPLICABLE',
      canReview: false,
      hasReview: false
    };
  }

  private async toFinalizedCard(
    reservation: IMyReservationSnapshot
  ): Promise<IReservationCardOutput> {
    const baseCard: IReservationCardOutput = {
      id: reservation.id,
      complexName: reservation.complexName,
      courtName: reservation.courtName,
      imageUrl: await this.tryCreateImageReadUrl(
        reservation.imageObjectKey,
        reservation.id
      ),
      startsAt: reservation.startsAt,
      endsAt: reservation.endsAt,
      status: 'COMPLETED',
      section: 'FINALIZED',
      reviewStatus: reservation.reviewId == null ? 'PENDING_REVIEW' : 'REVIEWED',
      canReview: reservation.reviewId == null,
      hasReview: reservation.reviewId != null
    };

    if (reservation.reviewId == null) {
      return {
        ...baseCard,
        primaryActionKey: 'leave_review',
        primaryActionLabel: LEAVE_REVIEW_LABEL
      };
    }

    return {
      ...baseCard,
      indicatorKey: 'already_reviewed',
      indicatorLabel: ALREADY_REVIEWED_LABEL
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
        'Unable to create reservation card image read URL.'
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
