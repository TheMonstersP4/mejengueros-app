import { ApiProperty } from '@nestjs/swagger';
import type { FilePurpose } from '../../../domain/enums/file-purpose.enum';
import { UploadedByResponse } from './confirm-upload.response';

/**
 * HTTP response body for uploaded image metadata.
 */
export class ImageUploadResponse {
  /**
   * Internal image upload ID.
   */
  @ApiProperty()
  id!: string;

  /**
   * Private S3 object key.
   */
  @ApiProperty()
  objectKey!: string;

  /**
   * Business purpose associated with the uploaded image.
   */
  @ApiProperty()
  purpose!: FilePurpose;

  /**
   * S3 object content type.
   */
  @ApiProperty()
  contentType!: string;

  /**
   * S3 object size in bytes.
   */
  @ApiProperty()
  sizeBytes!: number;

  /**
   * Short-lived URL for displaying the private image.
   */
  @ApiProperty()
  readUrl!: string;

  /**
   * Upload creation timestamp.
   */
  @ApiProperty()
  createdAt!: string;

  /**
   * User snapshot captured when the image was confirmed.
   */
  @ApiProperty({ type: () => UploadedByResponse })
  uploadedBy!: UploadedByResponse;
}
