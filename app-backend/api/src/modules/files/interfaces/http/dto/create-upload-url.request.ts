import { ApiProperty } from '@nestjs/swagger';
import { IsIn, IsInt, IsMimeType, IsPositive, Max } from 'class-validator';
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
    example: 524288,
    maximum: 8388608,
    minimum: 1
  })
  @IsInt()
  @IsPositive()
  @Max(8388608)
  sizeBytes!: number;
}
