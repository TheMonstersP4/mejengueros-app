import type { FilePurpose } from '../../domain/enums/file-purpose.enum';
import type { IUploadedByOutput } from './confirm-upload.output';

/**
 * Uploaded image response returned to API clients.
 */
export interface IListImageUploadsOutput {
  /**
   * Internal image upload ID.
   */
  id: string;

  /**
   * Private S3 object key.
   */
  objectKey: string;

  /**
   * Business purpose associated with the uploaded image.
   */
  purpose: FilePurpose;

  /**
   * S3 object content type.
   */
  contentType: string;

  /**
   * S3 object size in bytes.
   */
  sizeBytes: number;

  /**
   * Short-lived URL for displaying the private image.
   */
  readUrl: string;

  /**
   * Upload creation timestamp.
   */
  createdAt: string;

  /**
   * User snapshot captured when the image was confirmed.
   */
  uploadedBy: IUploadedByOutput;
}
