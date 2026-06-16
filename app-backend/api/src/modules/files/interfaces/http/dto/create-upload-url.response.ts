/**
 * HTTP response body for direct image upload URL creation.
 */
export class CreateUploadUrlResponse {
  /**
   * Private S3 object key the client must upload to.
   */
  objectKey!: string;

  /**
   * HTTP method expected by the storage endpoint.
   */
  method!: 'POST';

  /**
   * Short-lived presigned URL for uploading the object form.
   */
  uploadUrl!: string;

  /**
   * Form fields required by the storage provider.
   */
  fields!: Record<string, string>;

  /**
   * URL time-to-live in seconds.
   */
  expiresInSeconds!: number;

  /**
   * Maximum file size accepted for the selected upload purpose.
   */
  maxSizeBytes!: number;
}
