import { PROFILE_IMAGE_DEFAULT_MAX_BYTES } from '../modules/files/domain/constants/image-upload.constants';

/**
 * Storage settings for application-managed files.
 */
export interface IStorageConfig {
  /**
   * S3 bucket used for private application files.
   */
  bucketName: string;

  /**
   * AWS region where the application bucket exists.
   */
  region: string;

  /**
   * Prefix used to isolate object keys by environment or deployment.
   */
  keyPrefix: string;

  /**
   * Time-to-live in seconds for generated upload URLs.
   */
  uploadUrlTtlSeconds: number;

  /**
   * Maximum profile image upload size in bytes.
   */
  profileImageMaxBytes: number;

  /**
   * Allowed MIME types for image upload intents.
   */
  allowedImageMimeTypes: string[];
}

/**
 * Loads storage configuration from environment variables.
 *
 * @returns Storage config section.
 */
export function storageConfig(): IStorageConfig {
  return {
    bucketName: process.env.APP_S3_BUCKET_NAME ?? '',
    region: process.env.APP_S3_REGION ?? process.env.AWS_REGION ?? 'us-east-2',
    keyPrefix: process.env.APP_S3_KEY_PREFIX ?? 'uploads',
    uploadUrlTtlSeconds: Number(
      process.env.APP_S3_UPLOAD_URL_TTL_SECONDS ?? 300
    ),
    profileImageMaxBytes: Number(
      process.env.APP_S3_PROFILE_IMAGE_MAX_BYTES ??
        PROFILE_IMAGE_DEFAULT_MAX_BYTES
    ),
    allowedImageMimeTypes: (
      process.env.APP_S3_ALLOWED_IMAGE_MIME_TYPES ?? 'image/jpeg,image/png,image/webp'
    )
      .split(',')
      .map((type) => type.trim())
      .filter(Boolean)
  };
}
