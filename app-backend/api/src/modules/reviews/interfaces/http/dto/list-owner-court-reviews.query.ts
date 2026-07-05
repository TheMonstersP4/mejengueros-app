import { Type } from 'class-transformer';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsInt, IsOptional, IsUUID, Max, Min } from 'class-validator';
import {
  OWNER_REVIEWS_DEFAULT_PAGE_SIZE,
  OWNER_REVIEWS_MAX_PAGE,
  OWNER_REVIEWS_MAX_PAGE_SIZE
} from '../../../application/use-cases/list-owner-court-reviews.use-case';

/**
 * Query parameters for the owner reviews endpoint.
 */
export class ListOwnerCourtReviewsQuery {
  @ApiPropertyOptional({
    description:
      'Optional court identifier. Omit to return reviews for every court owned by the authenticated owner.',
    format: 'uuid'
  })
  @IsOptional()
  @IsUUID()
  courtId?: string;

  @ApiPropertyOptional({
    description:
      'One-based page number. Capped at 10000 to prevent huge offset abuse.',
    example: 1,
    minimum: 1,
    maximum: OWNER_REVIEWS_MAX_PAGE,
    default: 1
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(OWNER_REVIEWS_MAX_PAGE)
  page: number = 1;

  @ApiPropertyOptional({
    description: 'Number of reviews per page. Capped at 50.',
    example: 10,
    minimum: 1,
    maximum: OWNER_REVIEWS_MAX_PAGE_SIZE,
    default: OWNER_REVIEWS_DEFAULT_PAGE_SIZE
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(OWNER_REVIEWS_MAX_PAGE_SIZE)
  pageSize: number = OWNER_REVIEWS_DEFAULT_PAGE_SIZE;
}
