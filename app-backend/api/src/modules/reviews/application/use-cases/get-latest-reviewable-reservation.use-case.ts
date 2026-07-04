import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '@/modules/auth/application/dto/authenticated-user.output';
import {
  FILE_READ_URL_PORT,
  type IFileReadUrlPort
} from '@/modules/files/application/ports/file-read-url.port';
import { SyncAuthenticatedUserUseCase } from '@/modules/users/application/use-cases/sync-authenticated-user.use-case';
import type { ILatestReviewableReservationOutput } from '../dto/latest-reviewable-reservation.output';
import {
  REVIEW_REPOSITORY,
  type IReviewRepository
} from '../../domain/repositories/review.repository';

@Injectable()
export class GetLatestReviewableReservationUseCase {
  constructor(
    @Inject(REVIEW_REPOSITORY)
    private readonly reviewRepository: IReviewRepository,
    @Inject(SyncAuthenticatedUserUseCase)
    private readonly syncAuthenticatedUser: SyncAuthenticatedUserUseCase,
    @Inject(FILE_READ_URL_PORT)
    private readonly fileReadUrl: IFileReadUrlPort
  ) {}

  async execute(
    user: IAuthenticatedUserOutput
  ): Promise<ILatestReviewableReservationOutput | null> {
    const localUser = await this.syncAuthenticatedUser.execute(user);
    const reservation =
      await this.reviewRepository.findLatestReviewableReservationForUser(localUser.id);

    if (reservation == null) {
      return null;
    }

    const imageUrl =
      reservation.imageObjectKey == null
        ? undefined
        : await this.tryCreateImageReadUrl(reservation.imageObjectKey);

    return {
      reservationId: reservation.reservationId,
      complexName: reservation.complexName,
      courtName: reservation.courtName,
      startsAt: reservation.startsAt,
      endsAt: reservation.endsAt,
      imageUrl
    };
  }

  private async tryCreateImageReadUrl(objectKey: string): Promise<string | undefined> {
    try {
      return await this.fileReadUrl.createReadUrl(objectKey);
    } catch {
      return undefined;
    }
  }
}
