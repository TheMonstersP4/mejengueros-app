import { FilePurpose } from '../enums/file-purpose.enum';

/**
 * Properties required to rebuild an image upload entity from persistence.
 */
export interface IImageUploadEntityProps {
  /**
   * Internal image upload ID.
   */
  id: string;

  /**
   * Stable Cognito subject that owns the image.
   */
  ownerSub: string;

  /**
   * Email claim captured when the image was confirmed.
   */
  ownerEmail?: string | null;

  /**
   * Display name captured when the image was confirmed.
   */
  ownerName?: string | null;

  /**
   * Profile image claim captured when the image was confirmed.
   */
  ownerPictureUrl?: string | null;

  /**
   * Upstream identity provider captured when the image was confirmed.
   */
  ownerProvider?: string | null;

  /**
   * Business purpose associated with the image.
   */
  purpose: string;

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

  /**
   * Upload creation timestamp.
   */
  createdAt: Date;
}

/**
 * Snapshot returned by the image upload entity.
 */
export interface IImageUploadSnapshot {
  /**
   * Internal image upload ID.
   */
  id: string;

  /**
   * Stable Cognito subject that owns the image.
   */
  ownerSub: string;

  /**
   * Email claim captured when available.
   */
  ownerEmail?: string;

  /**
   * Display name captured when available.
   */
  ownerName?: string;

  /**
   * Profile image claim captured when available.
   */
  ownerPictureUrl?: string;

  /**
   * Upstream identity provider captured when available.
   */
  ownerProvider?: string;

  /**
   * Business purpose associated with the image.
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

  /**
   * Upload creation timestamp.
   */
  createdAt: Date;
}

/**
 * Uploaded image metadata persisted by the application.
 */
export class ImageUploadEntity {
  private constructor(private readonly props: IImageUploadEntityProps) {}

  /**
   * Rebuilds an image upload entity from a trusted persistence record.
   *
   * @param props - Image upload properties mapped from the database.
   * @returns Image upload entity ready for application use.
   */
  static fromPersistence(props: IImageUploadEntityProps): ImageUploadEntity {
    return new ImageUploadEntity(props);
  }

  /**
   * Converts the entity into a response-safe snapshot.
   *
   * @returns Uploaded image metadata.
   */
  toSnapshot(): IImageUploadSnapshot {
    return {
      id: this.props.id,
      ownerSub: this.props.ownerSub,
      ownerEmail: this.props.ownerEmail ?? undefined,
      ownerName: this.props.ownerName ?? undefined,
      ownerPictureUrl: this.props.ownerPictureUrl ?? undefined,
      ownerProvider: this.props.ownerProvider ?? undefined,
      purpose: FilePurpose.ProfileImage,
      objectKey: this.props.objectKey,
      contentType: this.props.contentType,
      sizeBytes: this.props.sizeBytes,
      createdAt: this.props.createdAt
    };
  }
}
