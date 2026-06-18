import { ApiProperty } from '@nestjs/swagger';
import { IsIn, IsString, MinLength } from 'class-validator';
import { FilePurpose } from '../../../domain/enums/file-purpose.enum';

/**
 * Request body for confirming a direct image upload.
 */
export class ConfirmUploadRequest {
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
   * Private S3 object key returned by upload URL creation.
   */
  @ApiProperty({
    description: 'Private S3 object key returned by upload URL creation.',
    example: 'dev/uploads/profile-image/user-sub/2026/06/image-id.jpg'
  })
  @IsString()
  @MinLength(1)
  objectKey!: string;
}
