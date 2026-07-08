import { Controller, Get, Inject, Param, ParseUUIDPipe, Query } from '@nestjs/common';
import { ApiOperation, ApiParam, ApiTags } from '@nestjs/swagger';
import {
  ApiEnvelopeErrors,
  ApiEnvelopeOk
} from '@/shared/interfaces/http/swagger/api-envelope.decorators';
import { withApiMeta } from '@/shared/interfaces/http/responses/api-response';
import { ListPublicCourtReviewsUseCase } from '../../../application/use-cases/list-public-court-reviews.use-case';
// eslint-disable-next-line @typescript-eslint/consistent-type-imports -- Nest needs DTO classes at runtime for validation metadata.
import { ListPublicCourtReviewsQuery } from '../dto/list-public-court-reviews.query';
import { PublicCourtReviewsResponse } from '../dto/public-court-reviews.response';

/**
 * Public HTTP endpoint that exposes the reviews of a single court so
 * players can read them from the public court detail before reserving.
 */
@ApiTags('courts')
@Controller('courts/:courtId/reviews')
export class PublicCourtReviewsController {
  constructor(
    @Inject(ListPublicCourtReviewsUseCase)
    private readonly listPublicCourtReviews: ListPublicCourtReviewsUseCase
  ) {}

  @Get()
  @ApiParam({ name: 'courtId', format: 'uuid' })
  @ApiOperation({
    summary: 'List the public reviews of a court.',
    description:
      'Returns the published reviews for a publicly visible court ordered from newest to oldest, together with a summary holding the total review count and the average rating. Reviewer identities are anonymized to a safe "Diego R." display name.'
  })
  @ApiEnvelopeOk(
    PublicCourtReviewsResponse,
    'Public court reviews wrapped in the API response envelope with pagination metadata.'
  )
  @ApiEnvelopeErrors(400, 404)
  async list(
    @Param('courtId', ParseUUIDPipe) courtId: string,
    @Query() query: ListPublicCourtReviewsQuery
  ): Promise<ReturnType<typeof withApiMeta<PublicCourtReviewsResponse>>> {
    const result = await this.listPublicCourtReviews.execute({
      courtId,
      page: query.page,
      pageSize: query.pageSize
    });

    const payload: PublicCourtReviewsResponse = {
      summary: result.summary,
      items: result.items
    };

    const totalPages = result.pageSize > 0 ? Math.ceil(result.totalItems / result.pageSize) : 0;

    return withApiMeta(payload, {
      pagination: {
        page: result.page,
        pageSize: result.pageSize,
        totalItems: result.totalItems,
        totalPages
      }
    });
  }
}
