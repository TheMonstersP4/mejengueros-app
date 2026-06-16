import { randomUUID } from 'crypto';
import { FilePurpose } from '../enums/file-purpose.enum';
import { FileOwnershipError } from '../errors/file-ownership.error';
import { FileTooLargeError } from '../errors/file-too-large.error';
import { InvalidFileObjectKeyError } from '../errors/invalid-file-object-key.error';
import { InvalidFilePurposeError } from '../errors/invalid-file-purpose.error';
import { UnsupportedFileTypeError } from '../errors/unsupported-file-type.error';

/**
 * Image upload policy values required by domain validation.
 */
export interface IImageUploadPolicyOptions {
  /**
   * Allowed image MIME types.
   */
  allowedMimeTypes: string[];

  /**
   * Maximum profile image size in bytes.
   */
  profileImageMaxBytes: number;

  /**
   * Prefix used for generated object keys.
   */
  keyPrefix: string;
}

/**
 * Validated image upload intent.
 */
export interface IValidatedImageUploadIntent {
  /**
   * File purpose requested by the client.
   */
  purpose: FilePurpose;

  /**
   * Client-declared content type.
   */
  contentType: string;

  /**
   * Client-declared file size in bytes.
   */
  sizeBytes: number;

  /**
   * Maximum size allowed for this intent.
   */
  maxSizeBytes: number;
}

/**
 * Validated uploaded image metadata.
 */
export interface IValidatedUploadedImage {
  /**
   * File purpose requested by the client.
   */
  purpose: FilePurpose;

  /**
   * Private object key confirmed by the API.
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
 * Validates image upload intents and builds private S3 object keys.
 */
export class ImageUploadPolicyService {
  private readonly allowedMimeTypes: Set<string>;

  constructor(private readonly options: IImageUploadPolicyOptions) {
    this.allowedMimeTypes = new Set(options.allowedMimeTypes);
  }

  /**
   * Validates a profile-image upload intent.
   *
   * @param input - Client upload intent.
   * @returns Normalized upload intent with policy limits.
   */
  validate(input: {
    purpose: string;
    contentType: string;
    sizeBytes: number;
  }): IValidatedImageUploadIntent {
    if (input.purpose !== FilePurpose.ProfileImage) {
      throw new InvalidFilePurposeError(input.purpose);
    }

    if (!this.allowedMimeTypes.has(input.contentType)) {
      throw new UnsupportedFileTypeError(input.contentType);
    }

    if (input.sizeBytes > this.options.profileImageMaxBytes) {
      throw new FileTooLargeError(
        input.sizeBytes,
        this.options.profileImageMaxBytes
      );
    }

    return {
      purpose: FilePurpose.ProfileImage,
      contentType: input.contentType,
      sizeBytes: input.sizeBytes,
      maxSizeBytes: this.options.profileImageMaxBytes
    };
  }

  /**
   * Builds a private object key for a validated image upload.
   *
   * @param input - Object key input.
   * @returns S3 object key.
   */
  buildObjectKey(input: {
    purpose: FilePurpose;
    ownerId: string;
    contentType: string;
    date?: Date;
    id?: string;
  }): string {
    const date = input.date ?? new Date();
    const year = date.getUTCFullYear();
    const month = String(date.getUTCMonth() + 1).padStart(2, '0');
    const extension = this.extensionFor(input.contentType);
    const objectId = input.id ?? randomUUID();
    const ownerId = this.normalizePathSegment(input.ownerId);
    const prefix = this.normalizePrefix(this.options.keyPrefix);

    return `${prefix}/${input.purpose}/${ownerId}/${year}/${month}/${objectId}.${extension}`;
  }

  /**
   * Validates that an uploaded object belongs to the current user and policy.
   *
   * @param input - Uploaded object metadata and ownership context.
   * @returns Confirmed uploaded image metadata.
   */
  validateUploadedObject(input: {
    purpose: string;
    ownerId: string;
    objectKey: string;
    contentType: string;
    sizeBytes: number;
    detectedContentType: string | null;
  }): IValidatedUploadedImage {
    const purpose = this.validateObjectKeyForOwner({
      purpose: input.purpose,
      ownerId: input.ownerId,
      objectKey: input.objectKey
    });

    if (!this.allowedMimeTypes.has(input.contentType)) {
      throw new UnsupportedFileTypeError(input.contentType);
    }

    if (input.detectedContentType !== input.contentType) {
      throw new UnsupportedFileTypeError(input.detectedContentType ?? 'unknown');
    }

    if (input.sizeBytes > this.options.profileImageMaxBytes) {
      throw new FileTooLargeError(
        input.sizeBytes,
        this.options.profileImageMaxBytes
      );
    }

    return {
      purpose,
      objectKey: input.objectKey,
      contentType: input.contentType,
      sizeBytes: input.sizeBytes
    };
  }

  /**
   * Validates that a file object key belongs to the current user.
   *
   * @param input - File ownership context.
   * @returns Validated file purpose.
   */
  validateObjectKeyForOwner(input: {
    purpose: string;
    ownerId: string;
    objectKey: string;
  }): FilePurpose {
    if (input.purpose !== FilePurpose.ProfileImage) {
      throw new InvalidFilePurposeError(input.purpose);
    }

    this.validateObjectKeyOwnership({
      purpose: FilePurpose.ProfileImage,
      ownerId: input.ownerId,
      objectKey: input.objectKey
    });

    return FilePurpose.ProfileImage;
  }

  private validateObjectKeyOwnership(input: {
    purpose: FilePurpose;
    ownerId: string;
    objectKey: string;
  }): void {
    if (!input.objectKey || input.objectKey.includes('..')) {
      throw new InvalidFileObjectKeyError(input.objectKey);
    }

    const prefix = this.normalizePrefix(this.options.keyPrefix);
    const ownerId = this.normalizePathSegment(input.ownerId);
    const expectedPrefix = `${prefix}/${input.purpose}/${ownerId}/`;

    if (!input.objectKey.startsWith(expectedPrefix)) {
      throw new FileOwnershipError(input.objectKey);
    }
  }

  private extensionFor(contentType: string): string {
    const extensionByType: Record<string, string> = {
      'image/jpeg': 'jpg',
      'image/png': 'png',
      'image/webp': 'webp'
    };

    return extensionByType[contentType] ?? 'bin';
  }

  private normalizePrefix(prefix: string): string {
    return prefix
      .split('/')
      .map((part) => this.normalizePathSegment(part))
      .filter(Boolean)
      .join('/');
  }

  private normalizePathSegment(value: string): string {
    return value.replace(/[^a-zA-Z0-9._-]/g, '-');
  }
}
