import { ApiProperty } from '@nestjs/swagger';
import { IsIn, IsInt, IsMimeType, IsPositive, Max } from 'class-validator';
import {
  PROFILE_IMAGE_EXAMPLE_BYTES,
  PROFILE_IMAGE_HARD_MAX_BYTES
} from '../../../domain/constants/image-upload.constants';
import { FilePurpose } from '../../../domain/enums/file-purpose.enum';

/**
 * Request body for creating an image upload URL.
 */
export class CreateUploadUrlRequest {
  /**
   * Business purpose for the uploaded image.
   */
  @ApiProperty({
    description: 'Business purpose for the uploaded image.',
    enum: FilePurpose,
    example: FilePurpose.ProfileImage
  })
  @IsIn([FilePurpose.ProfileImage])
  purpose!: FilePurpose;

  /**
   * Client-declared image MIME type.
   */
  @ApiProperty({
    description: 'Client-declared image MIME type.',
    example: 'image/jpeg'
  })
  @IsMimeType()
  contentType!: string;

  /**
   * Client-declared file size in bytes.
   */
  @ApiProperty({
    description: 'Client-declared file size in bytes.',
    example: PROFILE_IMAGE_EXAMPLE_BYTES,
    maximum: PROFILE_IMAGE_HARD_MAX_BYTES,
    minimum: 1
  })
  @IsInt()
  @IsPositive()
  @Max(PROFILE_IMAGE_HARD_MAX_BYTES)
  sizeBytes!: number;
}
