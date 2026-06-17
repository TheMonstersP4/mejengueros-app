import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { FilePurpose } from '../../../domain/enums/file-purpose.enum';

/**
 * HTTP response body for the image uploader snapshot.
 */
export class UploadedByResponse {
  /**
   * Stable Cognito subject.
   */
  @ApiProperty({ example: '21dbf550-b071-7037-4dc2-169c7a4b4c28' })
  sub!: string;

  /**
   * Email claim when available.
   */
  @ApiPropertyOptional({ example: 'player@example.com' })
  email?: string;

  /**
   * Display name when available.
   */
  @ApiPropertyOptional({ example: 'David Gutierrez' })
  name?: string;

  /**
   * Profile image URL when available.
   */
  @ApiPropertyOptional({ example: 'https://example.com/profile.jpg' })
  pictureUrl?: string;

  /**
   * Upstream identity provider when available.
   */
  @ApiPropertyOptional({ example: 'Google' })
  provider?: string;
}

/**
 * HTTP response body for confirmed direct image uploads.
 */
export class ConfirmUploadResponse {
  /**
   * Internal image upload ID.
   */
  @ApiProperty({ example: '01J0W5K4T2S8Q9B7J6V4M3N2P1' })
  id!: string;

  /**
   * Private S3 object key confirmed by the API.
   */
  @ApiProperty({
    example: 'dev/uploads/profile-image/user-sub/2026/06/image-id.jpg'
  })
  objectKey!: string;

  /**
   * Business purpose associated with the confirmed upload.
   */
  @ApiProperty({ enum: FilePurpose, example: FilePurpose.ProfileImage })
  purpose!: FilePurpose;

  /**
   * Current upload status.
   */
  @ApiProperty({ enum: ['ready'], example: 'ready' })
  status!: 'ready';

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
