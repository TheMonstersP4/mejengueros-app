import { Inject, Injectable } from '@nestjs/common';
import { ImageUploadPolicyService } from '../../domain/services/image-upload-policy.service';
import type { ICreateUploadUrlInput } from '../dto/create-upload-url.input';
import type { ICreateUploadUrlOutput } from '../dto/create-upload-url.output';
import type { IFileStoragePort } from '../ports/file-storage.port';
import { FILE_STORAGE_PORT } from '../ports/file-storage.port';
import { UPLOAD_URL_TTL_SECONDS } from '../tokens/upload-url-ttl-seconds.token';

/**
 * Creates direct-to-S3 upload URLs for validated image upload intents.
 */
@Injectable()
export class CreateUploadUrlUseCase {
  constructor(
    @Inject(FILE_STORAGE_PORT)
    private readonly fileStorage: IFileStoragePort,
    @Inject(ImageUploadPolicyService)
    private readonly imageUploadPolicy: ImageUploadPolicyService,
    @Inject(UPLOAD_URL_TTL_SECONDS)
    private readonly uploadUrlTtlSeconds: number
  ) {}

  /**
   * Validates an upload intent and returns a presigned upload URL.
   *
   * @param input - Upload intent submitted by an authenticated user.
   * @returns Presigned upload URL response.
   */
  async execute(input: ICreateUploadUrlInput): Promise<ICreateUploadUrlOutput> {
    const uploadIntent = this.imageUploadPolicy.validate(input);
    const objectKey = this.imageUploadPolicy.buildObjectKey({
      purpose: uploadIntent.purpose,
      ownerId: input.ownerSub,
      contentType: uploadIntent.contentType
    });
    const uploadForm = await this.fileStorage.createPresignedUploadUrl({
      objectKey,
      contentType: uploadIntent.contentType,
      expiresInSeconds: this.uploadUrlTtlSeconds,
      maxSizeBytes: uploadIntent.maxSizeBytes
    });

    return {
      objectKey,
      method: uploadForm.method,
      uploadUrl: uploadForm.uploadUrl,
      fields: uploadForm.fields,
      expiresInSeconds: this.uploadUrlTtlSeconds,
      maxSizeBytes: uploadIntent.maxSizeBytes
    };
  }
}
