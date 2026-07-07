import { ApiProperty } from '@nestjs/swagger';

/**
 * Safe reviewer display fields shown on the public court detail.
 */
export class PublicCourtReviewerResponse {
  @ApiProperty({ example: 'Diego R.' })
  displayName!: string;

  @ApiProperty({ example: 'DR' })
  initials!: string;
}

/**
 * Single review row shown on the public court detail.
 */
export class PublicCourtReviewItemResponse {
  @ApiProperty({ example: '8b8b2a04-4a52-4f3c-9d8f-1c0e1f81e0b1' })
  reviewId!: string;

  @ApiProperty({ example: 5, minimum: 1, maximum: 5 })
  rating!: number;

  @ApiProperty({ example: 'Great court and lighting.', nullable: true })
  comment!: string | null;

  @ApiProperty({ example: '2026-07-01T18:00:00.000Z' })
  createdAt!: string;

  @ApiProperty({ type: PublicCourtReviewerResponse })
  reviewer!: PublicCourtReviewerResponse;
}

/**
 * Aggregate rating summary for a court's public reviews.
 */
export class PublicCourtReviewsSummaryResponse {
  @ApiProperty({ example: 24, minimum: 0 })
  totalReviews!: number;

  @ApiProperty({
    description:
      'Average rating rounded to one decimal place. Null when the court has no reviews yet.',
    example: 4.6,
    nullable: true
  })
  averageRating!: number | null;
}

/**
 * Payload returned by the public court reviews endpoint.
 */
export class PublicCourtReviewsResponse {
  @ApiProperty({ type: PublicCourtReviewsSummaryResponse })
  summary!: PublicCourtReviewsSummaryResponse;

  @ApiProperty({ type: [PublicCourtReviewItemResponse] })
  items!: PublicCourtReviewItemResponse[];
}
