/**
 * Input required to create an image upload URL.
 */
export interface ICreateUploadUrlInput {
  /**
   * Authenticated Cognito subject requesting the upload.
   */
  ownerSub: string;

  /**
   * Business purpose for the file.
   */
  purpose: string;

  /**
   * Client-declared content type.
   */
  contentType: string;

  /**
   * Client-declared file size in bytes.
   */
  sizeBytes: number;
}
