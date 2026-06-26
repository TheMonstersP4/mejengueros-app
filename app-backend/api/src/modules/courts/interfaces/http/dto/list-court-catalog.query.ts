import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsString, IsUUID, MaxLength } from 'class-validator';

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
}
