/**
 * Upload URL response returned to API clients.
 */
export interface ICreateUploadUrlOutput {
  /**
   * Private S3 object key the client must upload to.
   */
  objectKey: string;

  /**
   * HTTP method expected by the storage endpoint.
   */
  method: 'POST';

  /**
   * Short-lived presigned URL for uploading the object form.
   */
  uploadUrl: string;

  /**
   * Form fields required by the storage provider.
   */
  fields: Record<string, string>;

  /**
   * URL time-to-live in seconds.
   */
  expiresInSeconds: number;

  /**
   * Maximum size accepted by the selected file policy.
   */
  maxSizeBytes: number;
}
