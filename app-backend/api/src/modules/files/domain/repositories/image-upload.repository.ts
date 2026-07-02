import type { FilePurpose } from '../enums/file-purpose.enum';
import type { ImageUploadEntity } from '../entities/image-upload.entity';

/**
 * Confirmed image upload data accepted by persistence adapters.
 */
export interface ISaveImageUploadInput {
  /**
   * Stable Cognito subject that owns the image.
   */
  ownerSub: string;

  /**
   * Email claim captured when the image was confirmed.
   */
  ownerEmail?: string;

  /**
   * Display name claim captured when the image was confirmed.
   */
  ownerName?: string;

  /**
   * Profile image claim captured when the image was confirmed.
   */
  ownerPictureUrl?: string;

  /**
   * Upstream identity provider captured when the image was confirmed.
   */
  ownerProvider?: string;

  /**
   * Business purpose associated with the uploaded image.
   */
  purpose: FilePurpose;

  /**
   * Private S3 object key.
   */
  objectKey: string;

  /**
   * Stored object content type.
   */
  contentType: string;

  /**
   * Stored object size in bytes.
   */
  sizeBytes: number;
}

/**
 * Persistence contract for uploaded image metadata.
 */
export interface IImageUploadRepository {
  /**
   * Finds one confirmed upload by its internal identifier.
   *
   * @param id - Image upload identifier.
   * @returns Image upload entity when found.
   */
  findById(id: string): Promise<ImageUploadEntity | null>;

  /**
   * Creates or updates metadata for a confirmed image upload.
   *
   * @param input - Confirmed upload metadata.
   * @returns Persisted image upload entity.
   */
  saveConfirmedUpload(input: ISaveImageUploadInput): Promise<ImageUploadEntity>;

  /**
   * Lists recently confirmed image uploads for one owner.
   *
   * @param ownerSub - Stable Cognito subject that owns the images.
   * @param limit - Maximum number of images to return.
   * @returns Image upload entities ordered by recent creation.
   */
  listRecent(ownerSub: string, limit: number): Promise<ImageUploadEntity[]>;
}

/**
 * Dependency injection token for the image upload repository port.
 */
export const IMAGE_UPLOAD_REPOSITORY = Symbol('IMAGE_UPLOAD_REPOSITORY');
