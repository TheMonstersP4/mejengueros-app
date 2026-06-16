import type { ImageUpload } from '../../../../generated/prisma/client';
import { ImageUploadEntity } from '../../domain/entities/image-upload.entity';

/**
 * Maps Prisma image upload records to domain entities.
 */
export class ImageUploadMapper {
  /**
   * Converts a Prisma image upload model into a domain entity.
   *
   * @param imageUpload - Image upload record returned by Prisma.
   * @returns Image upload entity used by the application layer.
   */
  static toDomain(imageUpload: ImageUpload): ImageUploadEntity {
    return ImageUploadEntity.fromPersistence({
      id: imageUpload.id,
      ownerSub: imageUpload.ownerSub,
      ownerEmail: imageUpload.ownerEmail,
      ownerName: imageUpload.ownerName,
      ownerPictureUrl: imageUpload.ownerPictureUrl,
      ownerProvider: imageUpload.ownerProvider,
      purpose: imageUpload.purpose,
      objectKey: imageUpload.objectKey,
      contentType: imageUpload.contentType,
      sizeBytes: imageUpload.sizeBytes,
      createdAt: imageUpload.createdAt
    });
  }
}
