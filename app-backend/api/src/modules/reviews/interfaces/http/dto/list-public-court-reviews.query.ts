import { Type } from 'class-transformer';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsInt, IsOptional, Max, Min } from 'class-validator';
import {
  PUBLIC_COURT_REVIEWS_DEFAULT_PAGE_SIZE,
  PUBLIC_COURT_REVIEWS_MAX_PAGE,
  PUBLIC_COURT_REVIEWS_MAX_PAGE_SIZE
} from '../../../application/use-cases/list-public-court-reviews.use-case';

/**
 * Query parameters for the public court reviews endpoint.
 */
export class ListPublicCourtReviewsQuery {
  @ApiPropertyOptional({
    description:
      'One-based page number. Capped at 10000 to prevent huge offset abuse.',
    example: 1,
    minimum: 1,
    maximum: PUBLIC_COURT_REVIEWS_MAX_PAGE,
    default: 1
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(PUBLIC_COURT_REVIEWS_MAX_PAGE)
  page: number = 1;

  @ApiPropertyOptional({
    description: 'Number of reviews per page. Capped at 50.',
    example: 10,
    minimum: 1,
    maximum: PUBLIC_COURT_REVIEWS_MAX_PAGE_SIZE,
    default: PUBLIC_COURT_REVIEWS_DEFAULT_PAGE_SIZE
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(PUBLIC_COURT_REVIEWS_MAX_PAGE_SIZE)
  pageSize: number = PUBLIC_COURT_REVIEWS_DEFAULT_PAGE_SIZE;
}
