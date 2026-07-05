import { Body, Controller, Get, Inject, Post, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiTags } from '@nestjs/swagger';
import type { IAuthenticatedUserOutput } from '@/modules/auth/application/dto/authenticated-user.output';
import { CognitoAuthGuard } from '@/modules/auth/interfaces/http/guards/cognito-auth.guard';
import { CurrentUser } from '@/shared/interfaces/http/decorators/current-user.decorator';
import {
  ApiEnvelopeCreated,
  ApiEnvelopeErrors,
  ApiEnvelopeOk
} from '@/shared/interfaces/http/swagger/api-envelope.decorators';
import { CreateReviewUseCase } from '../../../application/use-cases/create-review.use-case';
import { GetLatestReviewableReservationUseCase } from '../../../application/use-cases/get-latest-reviewable-reservation.use-case';
import { CreateReviewRequest } from '../dto/create-review.request';
import { CreateReviewResponse } from '../dto/create-review.response';
import { LatestReviewableReservationResponse } from '../dto/latest-reviewable-reservation.response';

@ApiTags('reviews')
@ApiBearerAuth()
@Controller('reviews')
export class ReviewsController {
  constructor(
    @Inject(CreateReviewUseCase)
    private readonly createReview: CreateReviewUseCase,
    @Inject(GetLatestReviewableReservationUseCase)
    private readonly getLatestReviewableReservation: GetLatestReviewableReservationUseCase
  ) {}

  @Get('latest-eligible-reservation')
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'Read the latest completed reservation pending review.',
    description:
      'Returns the latest authenticated player reservation that is completed, still missing a review, and can be used by the review launcher.'
  })
  @ApiEnvelopeOk(
    LatestReviewableReservationResponse,
    'Latest reviewable reservation wrapped in the API response envelope. Returns null data when the authenticated player has no eligible reservation pending review.',
    { nullableData: true }
  )
  @ApiEnvelopeErrors(401)
  async latestEligibleReservation(
    @CurrentUser() user: IAuthenticatedUserOutput
  ): Promise<LatestReviewableReservationResponse | null> {
    return this.getLatestReviewableReservation.execute(user);
  }

  @Post()
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'Create one review for one completed reservation.',
    description:
      'Requires an authenticated player, validates reservation ownership and completion, and enforces additional evidence rules for 1-star reviews.'
  })
  @ApiBody({ type: CreateReviewRequest })
  @ApiEnvelopeCreated(
    CreateReviewResponse,
    'Created review wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401, 404, 409)
  async create(
    @CurrentUser() user: IAuthenticatedUserOutput,
    @Body() request: CreateReviewRequest
  ): Promise<CreateReviewResponse> {
    return this.createReview.execute(user, request);
  }
}
