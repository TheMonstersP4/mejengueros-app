import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import type { FilePurpose } from '../../../domain/enums/file-purpose.enum';

/**
 * HTTP response body for confirmed direct image uploads.
 */
export class ConfirmUploadResponse {
  /**
   * Internal image upload ID.
   */
  @ApiProperty()
  id!: string;

  /**
   * Private S3 object key confirmed by the API.
   */
  @ApiProperty()
  objectKey!: string;

  /**
   * Business purpose associated with the confirmed upload.
   */
  @ApiProperty()
  purpose!: FilePurpose;

  /**
   * Current upload status.
   */
  @ApiProperty({ enum: ['ready'] })
  status!: 'ready';

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
  @ApiProperty()
  uploadedBy!: UploadedByResponse;
}

/**
 * HTTP response body for the image uploader snapshot.
 */
export class UploadedByResponse {
  /**
   * Stable Cognito subject.
   */
  @ApiProperty()
  sub!: string;

  /**
   * Email claim when available.
   */
  @ApiPropertyOptional()
  email?: string;

  /**
   * Display name when available.
   */
  @ApiPropertyOptional()
  name?: string;

  /**
   * Profile image URL when available.
   */
  @ApiPropertyOptional()
  pictureUrl?: string;

  /**
   * Upstream identity provider when available.
   */
  @ApiPropertyOptional()
  provider?: string;
}
