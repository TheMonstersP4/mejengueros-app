import { ApiProperty } from '@nestjs/swagger';

/**
 * Created complex payload returned by the endpoint.
 */
export class CreatedComplexResponse {
  @ApiProperty({ example: 'complex-id' })
  id!: string;

  @ApiProperty({ example: 'North Sports Center' })
  name!: string;

  @ApiProperty({ example: '123 Main Street, San José' })
  address!: string;

  @ApiProperty({ example: 'ACTIVE' })
  status!: string;

  @ApiProperty({ example: '2026-06-20T00:00:00.000Z' })
  createdAt!: string;

  @ApiProperty({ example: '2026-06-20T00:00:00.000Z' })
  updatedAt!: string;
}

/**
 * Created first-court payload returned by the endpoint.
 */
export class CreatedCourtResponse {
  @ApiProperty({ example: 'court-id' })
  id!: string;

  @ApiProperty({ example: 'complex-id' })
  complexId!: string;

  @ApiProperty({ example: 'Court A' })
  name!: string;

  @ApiProperty({ example: 'ACTIVE' })
  status!: string;

  @ApiProperty({ example: '2026-06-20T00:00:00.000Z' })
  createdAt!: string;

  @ApiProperty({ example: '2026-06-20T00:00:00.000Z' })
  updatedAt!: string;
}

/**
 * HTTP response body for complex creation.
 */
export class CreateComplexResponse {
  @ApiProperty({ type: CreatedComplexResponse })
  complex!: CreatedComplexResponse;

  @ApiProperty({ type: CreatedCourtResponse })
  firstCourt!: CreatedCourtResponse;
}
