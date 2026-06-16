/**
 * Input required to confirm a direct upload.
 */
export interface IConfirmUploadInput {
  /**
   * Cognito subject that owns the upload.
   */
  ownerSub: string;

  /**
   * Email claim captured when the upload is confirmed.
   */
  ownerEmail?: string;

  /**
   * Display name claim captured when the upload is confirmed.
   */
  ownerName?: string;

  /**
   * Profile image claim captured when the upload is confirmed.
   */
  ownerPictureUrl?: string;

  /**
   * Upstream identity provider captured when the upload is confirmed.
   */
  ownerProvider?: string;

  /**
   * Business purpose originally requested for the file.
   */
  purpose: string;

  /**
   * Private S3 object key to confirm.
   */
  objectKey: string;
}
