import { ApiProperty } from '@nestjs/swagger';

/**
 * Reviewer display fields shown to the owner in the dashboard.
 */
export class ReviewerDisplayResponse {
  @ApiProperty({ example: 'Diego R.' })
  displayName!: string;

  @ApiProperty({ example: 'DR' })
  initials!: string;
}

/**
 * Court context attached to a review.
 */
export class ReviewCourtSummaryResponse {
  @ApiProperty({ example: '0dd3a274-7d7b-45c6-a90d-4d14298ae7aa' })
  id!: string;

  @ApiProperty({ example: 'Cancha 1' })
  name!: string;
}

/**
 * Single review row in the owner reviews list.
 */
export class OwnerCourtReviewItemResponse {
  @ApiProperty({ example: '8b8b2a04-4a52-4f3c-9d8f-1c0e1f81e0b1' })
  reviewId!: string;

  @ApiProperty({ example: 5, minimum: 1, maximum: 5 })
  rating!: number;

  @ApiProperty({ example: 'Great court and lighting.', nullable: true })
  comment!: string | null;

  @ApiProperty({ example: '2026-07-01T18:00:00.000Z' })
  createdAt!: string;

  @ApiProperty({ type: ReviewCourtSummaryResponse })
  court!: ReviewCourtSummaryResponse;

  @ApiProperty({ type: ReviewerDisplayResponse })
  reviewer!: ReviewerDisplayResponse;
}

/**
 * Aggregate summary for the selected owner review scope.
 */
export class OwnerCourtReviewsSummaryResponse {
  @ApiProperty({
    description: 'Court filter applied to the response, or null when listing all owned courts.',
    example: '0dd3a274-7d7b-45c6-a90d-4d14298ae7aa',
    nullable: true
  })
  selectedCourtId!: string | null;

  @ApiProperty({ example: 24, minimum: 0 })
  totalReviews!: number;

  @ApiProperty({
    description:
      'Average rating rounded to one decimal place. Null when no reviews are available for the selection.',
    example: 4.6,
    nullable: true
  })
  averageRating!: number | null;
}

/**
 * Payload returned by the owner reviews endpoint.
 */
export class OwnerCourtReviewsResponse {
  @ApiProperty({ type: OwnerCourtReviewsSummaryResponse })
  summary!: OwnerCourtReviewsSummaryResponse;

  @ApiProperty({ type: [OwnerCourtReviewItemResponse] })
  items!: OwnerCourtReviewItemResponse[];
}
