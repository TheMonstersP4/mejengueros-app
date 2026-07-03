import { ApiProperty } from '@nestjs/swagger';

export class MyComplexHubCourtResponse {
  @ApiProperty({ example: 'court-id' })
  id!: string;

  @ApiProperty({ example: 'Court A' })
  name!: string;

  @ApiProperty({ example: 'ACTIVE' })
  status!: string;

  @ApiProperty({ example: 'CONFIGURED', enum: ['CONFIGURED', 'PENDING'] })
  availabilityStatus!: 'CONFIGURED' | 'PENDING';

  @ApiProperty({
    example: 'https://signed.example.test/courts/court-a.png',
    required: false,
    nullable: true
  })
  imageUrl?: string | null;
}

export class MyComplexHubComplexResponse {
  @ApiProperty({ example: 'complex-id' })
  id!: string;

  @ApiProperty({ example: 'North Sports Center' })
  name!: string;

  @ApiProperty({ example: '123 Main Street, San José' })
  address!: string;

  @ApiProperty({ example: 'province-id', required: false, nullable: true })
  provinceId?: string;

  @ApiProperty({ example: 'canton-id', required: false, nullable: true })
  cantonId?: string;

  @ApiProperty({ example: 9.935, required: false, nullable: true })
  latitude?: number;

  @ApiProperty({ example: -84.091, required: false, nullable: true })
  longitude?: number;

  @ApiProperty({ example: 'ACTIVE' })
  status!: string;

  @ApiProperty({ type: [MyComplexHubCourtResponse] })
  courts!: MyComplexHubCourtResponse[];
}

export class MyComplexHubResponse {
  @ApiProperty({ type: [MyComplexHubComplexResponse] })
  complexes!: MyComplexHubComplexResponse[];
}
