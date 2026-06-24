import { ApiProperty } from '@nestjs/swagger';

/**
 * Created complex payload returned by the endpoint.
 */
export class CreatedComplexResponse {
  @ApiProperty({ example: 'complex-id' })
  id!: string;

  @ApiProperty({ example: 'North Sports Center' })
  name!: string;

  @ApiProperty({ example: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1' })
  provinceId!: string;

  @ApiProperty({ example: '1f6adf24-ea42-4c49-9179-c5f73fef7a41' })
  cantonId!: string;

  @ApiProperty({ example: '123 Main Street, San José' })
  address!: string;

  @ApiProperty({ example: 9.935, required: false, nullable: true })
  latitude?: number;

  @ApiProperty({ example: -84.091, required: false, nullable: true })
  longitude?: number;

  @ApiProperty({
    type: [String],
    example: ['d76a5f20-83f0-4538-a1c8-4f7b60d0f4be']
  })
  serviceIds!: string[];

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

  @ApiProperty({
    type: [String],
    example: [
      'aab8a9f0-faf2-4e73-a8cb-6853f48cc9a7',
      'f96c0626-e055-4187-a100-c7d465f51f3b'
    ]
  })
  serviceIds!: string[];

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
