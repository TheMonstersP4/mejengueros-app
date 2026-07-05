import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '@/modules/auth/application/dto/authenticated-user.output';
import {
  IMAGE_UPLOAD_REPOSITORY,
  type IImageUploadRepository
} from '@/modules/files/domain/repositories/image-upload.repository';
import { SyncAuthenticatedUserUseCase } from '@/modules/users/application/use-cases/sync-authenticated-user.use-case';
import type { ICreateReviewInput } from '../dto/create-review.input';
import type { ICreateReviewOutput } from '../dto/create-review.output';
import { InvalidReviewRatingError } from '../../domain/errors/invalid-review-rating.error';
import { OneStarReviewCommentRequiredError } from '../../domain/errors/one-star-review-comment-required.error';
import { OneStarReviewEvidenceRequiredError } from '../../domain/errors/one-star-review-evidence-required.error';
import { ReviewAlreadyExistsError } from '../../domain/errors/review-already-exists.error';
import { ReviewReservationNotEligibleError } from '../../domain/errors/review-reservation-not-eligible.error';
import { ReviewReservationNotFoundError } from '../../domain/errors/review-reservation-not-found.error';
import {
  REVIEW_REPOSITORY,
  type IReviewRepository
} from '../../domain/repositories/review.repository';
import { validateReviewEvidenceUpload } from '../services/validate-review-evidence-upload';

@Injectable()
export class CreateReviewUseCase {
  constructor(
    @Inject(REVIEW_REPOSITORY)
    private readonly reviewRepository: IReviewRepository,
    @Inject(IMAGE_UPLOAD_REPOSITORY)
    private readonly imageUploadRepository: IImageUploadRepository,
    @Inject(SyncAuthenticatedUserUseCase)
    private readonly syncAuthenticatedUser: SyncAuthenticatedUserUseCase
  ) {}

  async execute(
    user: IAuthenticatedUserOutput,
    input: ICreateReviewInput
  ): Promise<ICreateReviewOutput> {
    if (!Number.isInteger(input.rating) || input.rating < 1 || input.rating > 5) {
      throw new InvalidReviewRatingError(input.rating);
    }

    const localUser = await this.syncAuthenticatedUser.execute(user);
    const reservation = await this.reviewRepository.findReservationById(input.reservationId);

    if (reservation == null || reservation.userId !== localUser.id) {
      throw new ReviewReservationNotFoundError(input.reservationId);
    }

    if (reservation.reviewId != null) {
      throw new ReviewAlreadyExistsError(input.reservationId);
    }

    if (reservation.status !== 'COMPLETED' || reservation.completedAt == null) {
      throw new ReviewReservationNotEligibleError(input.reservationId, reservation.status);
    }

    const normalizedComment = input.comment?.trim();

    if (input.rating === 1 && !normalizedComment) {
      throw new OneStarReviewCommentRequiredError();
    }

    if (input.rating === 1 && input.evidenceImageUploadId == null) {
      throw new OneStarReviewEvidenceRequiredError();
    }

    await validateReviewEvidenceUpload(
      this.reviewRepository,
      this.imageUploadRepository,
      user.sub,
      input.evidenceImageUploadId
    );

    return this.reviewRepository.createReview({
      reservationId: input.reservationId,
      rating: input.rating,
      comment: normalizedComment || undefined,
      evidenceImageUploadId: input.evidenceImageUploadId
    });
  }
}
