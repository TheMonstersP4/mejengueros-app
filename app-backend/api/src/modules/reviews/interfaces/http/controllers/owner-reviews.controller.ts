import { Controller, Get, Inject, Query, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import { CognitoAuthGuard } from '../../../../auth/interfaces/http/guards/cognito-auth.guard';
import type { IAuthenticatedUserOutput } from '../../../../auth/application/dto/authenticated-user.output';
import { CurrentUser } from '../../../../../shared/interfaces/http/decorators/current-user.decorator';
import {
  ApiEnvelopeErrors,
  ApiEnvelopeOk
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import { withApiMeta } from '../../../../../shared/interfaces/http/responses/api-response';
import { ListOwnerCourtReviewsUseCase } from '../../../application/use-cases/list-owner-court-reviews.use-case';
// eslint-disable-next-line @typescript-eslint/consistent-type-imports -- Nest needs DTO classes at runtime for validation metadata.
import { ListOwnerCourtReviewsQuery } from '../dto/list-owner-court-reviews.query';
import { OwnerCourtReviewsResponse } from '../dto/owner-court-reviews.response';

/**
 * HTTP endpoints for the owner reviews dashboard.
 */
@ApiTags('owners')
@ApiBearerAuth()
@Controller('owners/me/reviews')
export class OwnerReviewsController {
  constructor(
    @Inject(ListOwnerCourtReviewsUseCase)
    private readonly listOwnerCourtReviews: ListOwnerCourtReviewsUseCase
  ) {}

  @Get()
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'List reviews received for courts owned by the authenticated owner.',
    description:
      'Returns the reviews received for every court in the authenticated owner complexes, optionally filtered by `courtId`. The response includes a calculated summary with the total review count and the average rating for the selected scope, plus a paginated list of items ordered from newest to oldest.'
  })
  @ApiEnvelopeOk(
    OwnerCourtReviewsResponse,
    'Owner reviews wrapped in the API response envelope with pagination metadata.'
  )
  @ApiEnvelopeErrors(400, 401, 404)
  async list(
    @CurrentUser() user: IAuthenticatedUserOutput,
    @Query() query: ListOwnerCourtReviewsQuery
  ): Promise<ReturnType<typeof withApiMeta<OwnerCourtReviewsResponse>>> {
    const result = await this.listOwnerCourtReviews.execute(user, {
      courtId: query.courtId,
      page: query.page,
      pageSize: query.pageSize
    });

    const payload: OwnerCourtReviewsResponse = {
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
