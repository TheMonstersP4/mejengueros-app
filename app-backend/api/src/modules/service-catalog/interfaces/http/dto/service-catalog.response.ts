import { ApiProperty } from '@nestjs/swagger';
import { SERVICE_SCOPE_VALUES } from './list-services.query';

/**
 * Active service catalog item returned by the wizard endpoint.
 */
export class ServiceCatalogResponse {
  @ApiProperty({ example: 'd76a5f20-83f0-4538-a1c8-4f7b60d0f4be' })
  id!: string;

  @ApiProperty({ example: 'Lighting' })
  name!: string;

  @ApiProperty({ enum: SERVICE_SCOPE_VALUES, example: 'COURT' })
  scope!: (typeof SERVICE_SCOPE_VALUES)[number];
}
