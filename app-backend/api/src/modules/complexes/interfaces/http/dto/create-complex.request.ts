import { Transform, Type } from 'class-transformer';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import {
  ArrayNotEmpty,
  ArrayUnique,
  IsArray,
  IsDefined,
  IsNotEmpty,
  IsObject,
  IsOptional,
  IsString,
  IsUUID,
  MaxLength,
  Max,
  Min,
  ValidateNested
} from 'class-validator';

function trimText(value: unknown): unknown {
  return typeof value === 'string' ? value.trim() : value;
}

/**
 * Request body fields for the complex to create.
 */
export class CreateComplexBodyRequest {
  @ApiProperty({
    description: 'Commercial name of the sports complex.',
    example: 'North Sports Center'
  })
  @Transform(({ value }) => trimText(value))
  @IsString()
  @IsNotEmpty()
  @MaxLength(120)
  name!: string;

  @ApiProperty({
    description: 'Selected controlled province identifier.',
    example: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1'
  })
  @IsUUID()
  provinceId!: string;

  @ApiProperty({
    description: 'Selected canton identifier that must belong to the province.',
    example: '1f6adf24-ea42-4c49-9179-c5f73fef7a41'
  })
  @IsUUID()
  cantonId!: string;

  @ApiProperty({
    description: 'Address shown to players for the sports complex.',
    example: '123 Main Street, San José'
  })
  @Transform(({ value }) => trimText(value))
  @IsString()
  @IsNotEmpty()
  @MaxLength(255)
  address!: string;

  @ApiPropertyOptional({
    description: 'Latitude selected in the wizard map pin.',
    example: 9.935,
    minimum: -90,
    maximum: 90
  })
  @IsOptional()
  @Type(() => Number)
  @Min(-90)
  @Max(90)
  latitude?: number;

  @ApiPropertyOptional({
    description: 'Longitude selected in the wizard map pin.',
    example: -84.091,
    minimum: -180,
    maximum: 180
  })
  @IsOptional()
  @Type(() => Number)
  @Min(-180)
  @Max(180)
  longitude?: number;

  @ApiProperty({
    description: 'Active complex service identifiers selected in the wizard.',
    example: ['d76a5f20-83f0-4538-a1c8-4f7b60d0f4be'],
    type: [String]
  })
  @IsArray()
  @ArrayUnique()
  @IsUUID(undefined, { each: true })
  serviceIds!: string[];
}

/**
 * Request body fields for the first court to create.
 */
export class CreateFirstCourtBodyRequest {
  @ApiProperty({
    description: 'Display name of the first court inside the complex.',
    example: 'Court A'
  })
  @Transform(({ value }) => trimText(value))
  @IsString()
  @IsNotEmpty()
  @MaxLength(120)
  name!: string;

  @ApiProperty({
    description:
      'Active court service identifiers selected in the wizard, including grass type.',
    example: [
      'aab8a9f0-faf2-4e73-a8cb-6853f48cc9a7',
      'f96c0626-e055-4187-a100-c7d465f51f3b'
    ],
    type: [String]
  })
  @IsArray()
  @ArrayNotEmpty()
  @ArrayUnique()
  @IsUUID(undefined, { each: true })
  serviceIds!: string[];

  @ApiPropertyOptional({
    description: 'Optional confirmed upload identifier for the court image.',
    example: '9f6b4f0f-5f5a-4d8d-8c5e-2b2e7b0f6a3c'
  })
  @IsOptional()
  @IsUUID()
  imageUploadId?: string;
}

/**
 * Request body for creating a complex and its first court.
 */
export class CreateComplexRequest {
  @ApiProperty({
    description: 'Required complex data.',
    type: CreateComplexBodyRequest
  })
  @IsDefined()
  @IsObject()
  @ValidateNested()
  @Type(() => CreateComplexBodyRequest)
  complex!: CreateComplexBodyRequest;

  @ApiProperty({
    description: 'Required first court data.',
    type: CreateFirstCourtBodyRequest
  })
  @IsDefined()
  @IsObject()
  @ValidateNested()
  @Type(() => CreateFirstCourtBodyRequest)
  firstCourt!: CreateFirstCourtBodyRequest;
}
