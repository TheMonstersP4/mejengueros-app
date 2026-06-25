import { Inject, Injectable } from '@nestjs/common';
import type { ImageUpload } from '../../../../generated/prisma/client';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import type { ImageUploadEntity } from '../../domain/entities/image-upload.entity';
import type {
  IImageUploadRepository,
  ISaveImageUploadInput
} from '../../domain/repositories/image-upload.repository';
import { ImageUploadMapper } from '../mappers/image-upload.mapper';

/**
 * Prisma-backed implementation of uploaded image metadata persistence.
 */
@Injectable()
export class PrismaImageUploadRepository implements IImageUploadRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: PrismaService
  ) {}

  /**
   * Creates or updates metadata for a confirmed image upload.
   *
   * @param input - Confirmed upload metadata.
   * @returns Persisted image upload entity.
   */
  async saveConfirmedUpload(
    input: ISaveImageUploadInput
  ): Promise<ImageUploadEntity> {
    const imageUpload = await this.prisma.imageUpload.upsert({
      where: { objectKey: input.objectKey },
      create: input,
      update: {
        ownerEmail: input.ownerEmail,
        ownerName: input.ownerName,
        ownerPictureUrl: input.ownerPictureUrl,
        ownerProvider: input.ownerProvider,
        contentType: input.contentType,
        sizeBytes: input.sizeBytes
      }
    });

    return ImageUploadMapper.toDomain(imageUpload);
  }

  /**
   * Lists recently confirmed image uploads.
   *
   * @param limit - Maximum number of images to return.
   * @returns Image upload entities ordered by recent creation.
   */
  async listRecent(limit: number): Promise<ImageUploadEntity[]> {
    const imageUploads: ImageUpload[] = await this.prisma.imageUpload.findMany({
      orderBy: { createdAt: 'desc' },
      take: limit
    });

    return imageUploads.map((imageUpload) =>
      ImageUploadMapper.toDomain(imageUpload)
    );
  }
}
