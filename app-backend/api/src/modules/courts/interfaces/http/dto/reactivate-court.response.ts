import { ApiProperty } from '@nestjs/swagger';

export class ReactivatedCourtResponse {
  @ApiProperty({ example: '0dd3a274-7d7b-45c6-a90d-4d14298ae7aa' })
  id!: string;

  @ApiProperty({ example: 'Cancha 1' })
  name!: string;

  @ApiProperty({ example: 'ACTIVE', enum: ['ACTIVE', 'INACTIVE'] })
  status!: string;

  @ApiProperty({ example: '2026-08-04T02:14:00.000Z' })
  updatedAt!: string;
}

export class ReactivateCourtResponse {
  @ApiProperty({ type: ReactivatedCourtResponse })
  court!: ReactivatedCourtResponse;
}
