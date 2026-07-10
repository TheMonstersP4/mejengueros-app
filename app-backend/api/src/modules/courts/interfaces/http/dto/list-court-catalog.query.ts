import { Type } from 'class-transformer';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsInt, IsOptional, IsString, IsUUID, Max, MaxLength, Min } from 'class-validator';
import {
  PUBLIC_COURT_CATALOG_DEFAULT_PAGE_SIZE,
  PUBLIC_COURT_CATALOG_MAX_PAGE,
  PUBLIC_COURT_CATALOG_MAX_PAGE_SIZE
} from '../../../application/use-cases/list-public-court-catalog.use-case';

export class ListCourtCatalogQuery {
  @ApiPropertyOptional({
    description: 'Optional free-text search over court and complex names.',
    example: 'Nogales'
  })
  @IsOptional()
  @IsString()
  @MaxLength(100)
  q?: string;

  @ApiPropertyOptional({
    description: 'Optional province identifier filter.',
    format: 'uuid'
  })
  @IsOptional()
  @IsUUID()
  provinceId?: string;

  @ApiPropertyOptional({
    description: 'Optional canton identifier filter.',
    format: 'uuid'
  })
  @IsOptional()
  @IsUUID()
  cantonId?: string;

  @ApiPropertyOptional({
    description:
      'One-based page number. Capped at 10000 to prevent huge offset abuse.',
    example: 1,
    minimum: 1,
    maximum: PUBLIC_COURT_CATALOG_MAX_PAGE,
    default: 1
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(PUBLIC_COURT_CATALOG_MAX_PAGE)
  page: number = 1;

  @ApiPropertyOptional({
    description: 'Number of courts per page. Capped at 50.',
    example: 10,
    minimum: 1,
    maximum: PUBLIC_COURT_CATALOG_MAX_PAGE_SIZE,
    default: PUBLIC_COURT_CATALOG_DEFAULT_PAGE_SIZE
  })
  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(PUBLIC_COURT_CATALOG_MAX_PAGE_SIZE)
  pageSize: number = PUBLIC_COURT_CATALOG_DEFAULT_PAGE_SIZE;
}
