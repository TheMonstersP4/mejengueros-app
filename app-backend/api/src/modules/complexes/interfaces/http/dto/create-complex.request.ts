import { Transform, Type } from 'class-transformer';
import { ApiProperty } from '@nestjs/swagger';
import {
  IsDefined,
  IsNotEmpty,
  IsObject,
  IsString,
  MaxLength,
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
    description: 'Address shown to players for the sports complex.',
    example: '123 Main Street, San José'
  })
  @Transform(({ value }) => trimText(value))
  @IsString()
  @IsNotEmpty()
  @MaxLength(255)
  address!: string;
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
