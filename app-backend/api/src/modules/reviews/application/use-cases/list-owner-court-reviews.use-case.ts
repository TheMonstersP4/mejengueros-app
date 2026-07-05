import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import {
  REVIEW_REPOSITORY,
  type IListOwnerCourtReviewsQuery,
  type IListOwnerCourtReviewsResult,
  type IReviewRepository
} from '../../domain/repositories/review.repository';

/**
 * Maximum allowed `pageSize` for the owner reviews endpoint.
 */
export const OWNER_REVIEWS_MAX_PAGE_SIZE = 50;

/**
 * Default `pageSize` for the owner reviews endpoint when omitted.
 */
export const OWNER_REVIEWS_DEFAULT_PAGE_SIZE = 10;

/**
 * Maximum allowed `page` number for the owner reviews endpoint.
 *
 * Caps the `OFFSET` produced by `(page - 1) * pageSize` to prevent huge
 * offset abuse against the underlying review table.
 */
export const OWNER_REVIEWS_MAX_PAGE = 10_000;

/**
 * Request shape accepted by the owner reviews use case.
 */
export interface IListOwnerCourtReviewsRequest {
  page: number;
  pageSize: number;
  courtId?: string;
}

/**
 * Returns the paginated list of reviews received for courts owned by the
 * authenticated owner, optionally filtered to a single court.
 */
@Injectable()
export class ListOwnerCourtReviewsUseCase {
  constructor(
    @Inject(REVIEW_REPOSITORY)
    private readonly reviewRepository: IReviewRepository
  ) {}

  async execute(
    user: IAuthenticatedUserOutput,
    request: IListOwnerCourtReviewsRequest
  ): Promise<IListOwnerCourtReviewsResult> {
    const query: IListOwnerCourtReviewsQuery = {
      ownerIdentity: {
        sub: user.sub,
        provider: user.provider
      },
      ...(request.courtId != null ? { court: { courtId: request.courtId } } : {}),
      pagination: {
        page: request.page,
        pageSize: request.pageSize
      }
    };

    return this.reviewRepository.listOwnerCourtReviews(query);
  }
}
