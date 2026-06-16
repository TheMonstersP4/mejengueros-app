import type { FilePurpose } from '../../domain/enums/file-purpose.enum';

/**
 * Confirmed upload response returned to API clients.
 */
export interface IConfirmUploadOutput {
  /**
   * Internal image upload ID.
   */
  id: string;

  /**
   * Private S3 object key confirmed by the API.
   */
  objectKey: string;

  /**
   * Business purpose associated with the confirmed file.
   */
  purpose: FilePurpose;

  /**
   * Current upload status.
   */
  status: 'ready';

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

/**
 * User snapshot attached to uploaded image responses.
 */
export interface IUploadedByOutput {
  /**
   * Stable Cognito subject.
   */
  sub: string;

  /**
   * Email claim when available.
   */
  email?: string;

  /**
   * Display name when available.
   */
  name?: string;

  /**
   * Profile image URL when available.
   */
  pictureUrl?: string;

  /**
   * Upstream identity provider when available.
   */
  provider?: string;
}
