import { ApiProperty } from '@nestjs/swagger';
import { IsUUID } from 'class-validator';

/**
 * Request body for replacing the authenticated user's custom profile image.
 */
export class UpdateProfileImageRequest {
  @ApiProperty({
    description: 'Confirmed profile image upload identifier.',
    format: 'uuid',
    example: 'd55d773e-d6e9-4f67-aa1a-87f64dfd79c2'
  })
  @IsUUID()
  imageUploadId!: string;
}
