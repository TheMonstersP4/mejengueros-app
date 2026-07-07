import { Inject, Injectable } from '@nestjs/common';
import { PublicCourtReviewsCourtNotFoundError } from '../../domain/errors/public-court-not-found.error';
import {
  REVIEW_REPOSITORY,
  type IListPublicCourtReviewsResult,
  type IReviewRepository
} from '../../domain/repositories/review.repository';

/**
 * Default `pageSize` for the public court reviews endpoint when omitted.
 */
export const PUBLIC_COURT_REVIEWS_DEFAULT_PAGE_SIZE = 10;

/**
 * Maximum allowed `pageSize` for the public court reviews endpoint.
 */
export const PUBLIC_COURT_REVIEWS_MAX_PAGE_SIZE = 50;

/**
 * Maximum allowed `page` number for the public court reviews endpoint.
 *
 * Caps the `OFFSET` produced by `(page - 1) * pageSize` to prevent huge
 * offset abuse against the underlying review table.
 */
export const PUBLIC_COURT_REVIEWS_MAX_PAGE = 10_000;

/**
 * Request shape accepted by the public court reviews use case.
 */
export interface IListPublicCourtReviewsRequest {
  courtId: string;
  page: number;
  pageSize: number;
}

/**
 * Returns the paginated list of published reviews for a publicly visible
 * court, along with the aggregate rating summary. Any authenticated or
 * anonymous player can read them from the public court detail.
 */
@Injectable()
export class ListPublicCourtReviewsUseCase {
  constructor(
    @Inject(REVIEW_REPOSITORY)
    private readonly reviewRepository: IReviewRepository
  ) {}

  async execute(
    request: IListPublicCourtReviewsRequest
  ): Promise<IListPublicCourtReviewsResult> {
    const visibleCourtId = await this.reviewRepository.findPubliclyVisibleCourtId(
      request.courtId
    );

    if (visibleCourtId == null) {
      throw new PublicCourtReviewsCourtNotFoundError(request.courtId);
    }

    return this.reviewRepository.listPublicCourtReviews({
      courtId: visibleCourtId,
      pagination: {
        page: request.page,
        pageSize: request.pageSize
      }
    });
  }
}
