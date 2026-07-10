import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsOptional, IsUUID } from 'class-validator';

/**
 * Query parameters for the owner reservations endpoint.
 */
export class ListOwnerReservationsQuery {
  @ApiPropertyOptional({
    description:
      'Optional court identifier. Omit to return reservations for every court owned by the authenticated owner.',
    format: 'uuid'
  })
  @IsOptional()
  @IsUUID()
  courtId?: string;
}
