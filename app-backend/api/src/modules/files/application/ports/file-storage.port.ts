/**
 * Upload form request sent to file storage providers.
 */
export interface ICreatePresignedUploadUrlInput {
  /**
   * Private storage object key.
   */
  objectKey: string;

  /**
   * MIME type expected by the upload request.
   */
  contentType: string;

  /**
   * URL time-to-live in seconds.
   */
  expiresInSeconds: number;

  /**
   * Maximum object size accepted by the storage policy.
   */
  maxSizeBytes: number;
}

/**
 * Storage adapter response for upload form creation.
 */
export interface ICreatePresignedUploadUrlOutput {
  /**
   * HTTP method expected by the storage endpoint.
   */
  method: 'POST';

  /**
   * Short-lived URL for direct object upload form submission.
   */
  uploadUrl: string;

  /**
   * Form fields required by the storage provider.
   */
  fields: Record<string, string>;
}

/**
 * Object inspection request sent to file storage providers.
 */
export interface IInspectUploadedObjectInput {
  /**
   * Private storage object key.
   */
  objectKey: string;
}

/**
 * Storage metadata and byte signature detected for an uploaded object.
 */
export interface IInspectUploadedObjectOutput {
  /**
   * Content type stored by the provider.
   */
  contentType: string;

  /**
   * Object size in bytes.
   */
  sizeBytes: number;

  /**
   * MIME type detected from the first bytes of the object.
   */
  detectedContentType: string | null;
}

/**
 * Read URL request sent to file storage providers.
 */
export interface ICreatePresignedReadUrlInput {
  /**
   * Private storage object key.
   */
  objectKey: string;

  /**
   * URL time-to-live in seconds.
   */
  expiresInSeconds: number;
}

/**
 * Storage adapter response for private object reads.
 */
export interface ICreatePresignedReadUrlOutput {
  /**
   * Short-lived URL for reading the private object.
   */
  readUrl: string;
}

/**
 * Port implemented by file storage providers.
 */
export interface IFileStoragePort {
  /**
   * Creates a presigned URL for direct file upload.
   *
   * @param input - Upload URL request.
   * @returns Presigned upload form.
   */
  createPresignedUploadUrl(
    input: ICreatePresignedUploadUrlInput
  ): Promise<ICreatePresignedUploadUrlOutput>;

  /**
   * Reads storage metadata and a small byte signature for an uploaded object.
   *
   * @param input - Object inspection request.
   * @returns Uploaded object metadata and detected content type.
   */
  inspectUploadedObject(
    input: IInspectUploadedObjectInput
  ): Promise<IInspectUploadedObjectOutput>;

  /**
   * Creates a presigned URL for reading a private file.
   *
   * @param input - Read URL request.
   * @returns Presigned read URL.
   */
  createPresignedReadUrl(
    input: ICreatePresignedReadUrlInput
  ): Promise<ICreatePresignedReadUrlOutput>;
}

/**
 * Injection token for the file storage port.
 */
export const FILE_STORAGE_PORT = Symbol('FILE_STORAGE_PORT');
