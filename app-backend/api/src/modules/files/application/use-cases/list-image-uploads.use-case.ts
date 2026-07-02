import { Inject, Injectable } from '@nestjs/common';
import type { IImageUploadSnapshot } from '../../domain/entities/image-upload.entity';
import type { IImageUploadRepository } from '../../domain/repositories/image-upload.repository';
import { IMAGE_UPLOAD_REPOSITORY } from '../../domain/repositories/image-upload.repository';
import type { IListImageUploadsOutput } from '../dto/list-image-uploads.output';
import type { IFileStoragePort } from '../ports/file-storage.port';
import { FILE_STORAGE_PORT } from '../ports/file-storage.port';
import { READ_URL_TTL_SECONDS } from '../tokens/read-url-ttl-seconds.token';

const DEFAULT_IMAGE_UPLOAD_LIST_LIMIT = 50;

/**
 * Lists confirmed image uploads with temporary read URLs.
 */
@Injectable()
export class ListImageUploadsUseCase {
  constructor(
    @Inject(IMAGE_UPLOAD_REPOSITORY)
    private readonly imageUploadRepository: IImageUploadRepository,
    @Inject(FILE_STORAGE_PORT)
    private readonly fileStorage: IFileStoragePort,
    @Inject(READ_URL_TTL_SECONDS)
    private readonly readUrlTtlSeconds: number
  ) {}

  /**
   * Returns recent uploaded images for one authenticated owner.
   *
   * @param ownerSub - Stable Cognito subject that owns the uploads.
   * @returns Recent image uploads with read URLs.
   */
  async execute(ownerSub: string): Promise<IListImageUploadsOutput[]> {
    const imageUploads = await this.imageUploadRepository.listRecent(
      ownerSub,
      DEFAULT_IMAGE_UPLOAD_LIST_LIMIT
    );

    return Promise.all(
      imageUploads.map(async (imageUpload) =>
        this.toOutput(imageUpload.toSnapshot())
      )
    );
  }

  private async toOutput(
    imageUpload: IImageUploadSnapshot
  ): Promise<IListImageUploadsOutput> {
    const read = await this.fileStorage.createPresignedReadUrl({
      objectKey: imageUpload.objectKey,
      expiresInSeconds: this.readUrlTtlSeconds
    });

    return {
      id: imageUpload.id,
      objectKey: imageUpload.objectKey,
      purpose: imageUpload.purpose,
      contentType: imageUpload.contentType,
      sizeBytes: imageUpload.sizeBytes,
      readUrl: read.readUrl,
      createdAt: imageUpload.createdAt.toISOString(),
      uploadedBy: {
        sub: imageUpload.ownerSub,
        email: imageUpload.ownerEmail,
        name: imageUpload.ownerName,
        pictureUrl: imageUpload.ownerPictureUrl,
        provider: imageUpload.ownerProvider
      }
    };
  }
}
