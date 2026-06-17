import { ApiProperty } from '@nestjs/swagger';
import { FilePurpose } from '../../../domain/enums/file-purpose.enum';
import { UploadedByResponse } from './confirm-upload.response';

/**
 * HTTP response body for uploaded image metadata.
 */
export class ImageUploadResponse {
  /**
   * Internal image upload ID.
   */
  @ApiProperty({ example: '01J0W5K4T2S8Q9B7J6V4M3N2P1' })
  id!: string;

  /**
   * Private S3 object key.
   */
  @ApiProperty({
    example: 'dev/uploads/profile-image/user-sub/2026/06/image-id.jpg'
  })
  objectKey!: string;

  /**
   * Business purpose associated with the uploaded image.
   */
  @ApiProperty({ enum: FilePurpose, example: FilePurpose.ProfileImage })
  purpose!: FilePurpose;

  /**
   * S3 object content type.
   */
  @ApiProperty({ example: 'image/jpeg' })
  contentType!: string;

  /**
   * S3 object size in bytes.
   */
  @ApiProperty({ example: 524288 })
  sizeBytes!: number;

  /**
   * Short-lived URL for displaying the private image.
   */
  @ApiProperty({ example: 'https://bucket.s3.amazonaws.com/signed-read-url' })
  readUrl!: string;

  /**
   * Upload creation timestamp.
   */
  @ApiProperty({ example: '2026-06-17T18:20:00.000Z' })
  createdAt!: string;

  /**
   * User snapshot captured when the image was confirmed.
   */
  @ApiProperty({ type: () => UploadedByResponse })
  uploadedBy!: UploadedByResponse;
}
