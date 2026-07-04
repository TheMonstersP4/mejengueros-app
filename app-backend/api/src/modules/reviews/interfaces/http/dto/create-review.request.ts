import { ApiProperty } from '@nestjs/swagger';
import {
  IsInt,
  IsOptional,
  IsString,
  IsUUID,
  Max,
  Min,
  MinLength
} from 'class-validator';

export class CreateReviewRequest {
  @ApiProperty({ format: 'uuid' })
  @IsUUID()
  reservationId!: string;

  @ApiProperty({ minimum: 1, maximum: 5, example: 1 })
  @IsInt()
  @Min(1)
  @Max(5)
  rating!: number;

  @ApiProperty({
    required: false,
    example: 'La iluminación falló toda la hora y la cancha estaba muy resbalosa.'
  })
  @IsOptional()
  @IsString()
  comment?: string;

  @ApiProperty({ required: false, format: 'uuid' })
  @IsOptional()
  @IsUUID()
  @MinLength(1)
  evidenceImageUploadId?: string;
}
