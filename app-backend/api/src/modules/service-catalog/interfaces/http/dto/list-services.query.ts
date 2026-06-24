import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsIn, IsOptional } from 'class-validator';
import type { IServiceCatalogScope } from '../../../domain/repositories/service-catalog.repository';

export const SERVICE_SCOPE_VALUES = ['COMPLEX', 'COURT'] as const;

/**
 * Optional service-catalog filters accepted by the wizard.
 */
export class ListServicesQuery {
  @ApiPropertyOptional({
    description: 'Optional service scope filter.',
    enum: SERVICE_SCOPE_VALUES,
    example: 'COURT'
  })
  @IsOptional()
  @IsIn(SERVICE_SCOPE_VALUES)
  scope?: IServiceCatalogScope;
}
