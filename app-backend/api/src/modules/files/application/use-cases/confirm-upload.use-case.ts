import { Inject, Injectable } from '@nestjs/common';
import type { IImageUploadRepository } from '../../domain/repositories/image-upload.repository';
import { IMAGE_UPLOAD_REPOSITORY } from '../../domain/repositories/image-upload.repository';
import { ImageUploadPolicyService } from '../../domain/services/image-upload-policy.service';
import type { IConfirmUploadInput } from '../dto/confirm-upload.input';
import type { IConfirmUploadOutput } from '../dto/confirm-upload.output';
import type { IFileStoragePort } from '../ports/file-storage.port';
import { FILE_STORAGE_PORT } from '../ports/file-storage.port';
import { READ_URL_TTL_SECONDS } from '../tokens/read-url-ttl-seconds.token';

/**
 * Confirms that a direct upload exists and matches the image policy.
 */
@Injectable()
export class ConfirmUploadUseCase {
  constructor(
    @Inject(FILE_STORAGE_PORT)
    private readonly fileStorage: IFileStoragePort,
    @Inject(ImageUploadPolicyService)
    private readonly imageUploadPolicy: ImageUploadPolicyService,
    @Inject(IMAGE_UPLOAD_REPOSITORY)
    private readonly imageUploadRepository: IImageUploadRepository,
    @Inject(READ_URL_TTL_SECONDS)
    private readonly readUrlTtlSeconds: number
  ) {}

  /**
   * Confirms an uploaded object and returns its safe metadata.
   *
   * @param input - Upload confirmation request.
   * @returns Confirmed upload metadata.
   */
  async execute(input: IConfirmUploadInput): Promise<IConfirmUploadOutput> {
    this.imageUploadPolicy.validateObjectKeyForOwner({
      purpose: input.purpose,
      ownerId: input.ownerSub,
      objectKey: input.objectKey
    });
    const inspected = await this.fileStorage.inspectUploadedObject({
      objectKey: input.objectKey
    });
    const uploadedImage = this.imageUploadPolicy.validateUploadedObject({
      purpose: input.purpose,
      ownerId: input.ownerSub,
      objectKey: input.objectKey,
      contentType: inspected.contentType,
      sizeBytes: inspected.sizeBytes,
      detectedContentType: inspected.detectedContentType
    });
    const savedImage = await this.imageUploadRepository.saveConfirmedUpload({
      ownerSub: input.ownerSub,
      ownerEmail: input.ownerEmail,
      ownerName: input.ownerName,
      ownerPictureUrl: input.ownerPictureUrl,
      ownerProvider: input.ownerProvider,
      purpose: uploadedImage.purpose,
      objectKey: uploadedImage.objectKey,
      contentType: uploadedImage.contentType,
      sizeBytes: uploadedImage.sizeBytes
    });
    const snapshot = savedImage.toSnapshot();
    const read = await this.fileStorage.createPresignedReadUrl({
      objectKey: snapshot.objectKey,
      expiresInSeconds: this.readUrlTtlSeconds
    });

    return {
      id: snapshot.id,
      objectKey: snapshot.objectKey,
      purpose: snapshot.purpose,
      status: 'ready',
      contentType: snapshot.contentType,
      sizeBytes: snapshot.sizeBytes,
      readUrl: read.readUrl,
      createdAt: snapshot.createdAt.toISOString(),
      uploadedBy: {
        sub: snapshot.ownerSub,
        email: snapshot.ownerEmail,
        name: snapshot.ownerName,
        pictureUrl: snapshot.ownerPictureUrl,
        provider: snapshot.ownerProvider
      }
    };
  }
}
