import { ApiProperty } from '@nestjs/swagger';
import { PROFILE_IMAGE_DEFAULT_MAX_BYTES } from '../../../domain/constants/image-upload.constants';

/**
 * HTTP response body for direct image upload URL creation.
 */
export class CreateUploadUrlResponse {
  /**
   * Private S3 object key the client must upload to.
   */
  @ApiProperty({
    description: 'Private S3 object key the client must upload to.',
    example: 'dev/uploads/profile-image/user-sub/2026/06/image-id.jpg'
  })
  objectKey!: string;

  /**
   * HTTP method expected by the storage endpoint.
   */
  @ApiProperty({ description: 'HTTP method expected by the storage endpoint.', example: 'POST' })
  method!: 'POST';

  /**
   * Short-lived presigned URL for uploading the object form.
   */
  @ApiProperty({
    description: 'Short-lived presigned URL for uploading the object form.',
    example: 'https://bucket.s3.amazonaws.com/'
  })
  uploadUrl!: string;

  /**
   * Form fields required by the storage provider.
   */
  @ApiProperty({
    additionalProperties: { type: 'string' },
    description: 'Form fields required by the storage provider.',
    example: {
      key: 'dev/uploads/profile-image/user-sub/2026/06/image-id.jpg',
      policy: 'base64-policy',
      'x-amz-algorithm': 'AWS4-HMAC-SHA256'
    },
    type: 'object'
  })
  fields!: Record<string, string>;

  /**
   * URL time-to-live in seconds.
   */
  @ApiProperty({
    description: 'URL time-to-live in seconds.',
    example: 300
  })
  expiresInSeconds!: number;

  /**
   * Maximum file size accepted for the selected upload purpose.
   */
  @ApiProperty({
    description: 'Maximum file size accepted for the selected upload purpose.',
    example: PROFILE_IMAGE_DEFAULT_MAX_BYTES
  })
  maxSizeBytes!: number;
}
