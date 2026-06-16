import {
  GetObjectCommand,
  HeadObjectCommand,
  type GetObjectCommandOutput,
  type S3Client
} from '@aws-sdk/client-s3';
import { createPresignedPost } from '@aws-sdk/s3-presigned-post';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import { Inject, Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { S3_CLIENT } from '../../../../shared/infrastructure/storage/s3-client.provider';
import type {
  ICreatePresignedUploadUrlInput,
  ICreatePresignedUploadUrlOutput,
  ICreatePresignedReadUrlInput,
  ICreatePresignedReadUrlOutput,
  IFileStoragePort,
  IInspectUploadedObjectInput,
  IInspectUploadedObjectOutput
} from '../../application/ports/file-storage.port';
import { StorageInspectionError } from '../errors/storage-inspection.error';
import { StorageObjectNotFoundError } from '../errors/storage-object-not-found.error';
import { StorageUploadUrlError } from '../errors/storage-upload-url.error';

/**
 * S3 implementation of browser upload form creation.
 */
@Injectable()
export class S3FileStorageAdapter implements IFileStoragePort {
  constructor(
    @Inject(S3_CLIENT)
    private readonly s3Client: S3Client,
    @Inject(ConfigService)
    private readonly configService: ConfigService
  ) {}

  /**
   * Creates a presigned POST form for direct S3 uploads.
   *
   * @param input - Upload URL request.
   * @returns Presigned upload form.
   */
  async createPresignedUploadUrl(
    input: ICreatePresignedUploadUrlInput
  ): Promise<ICreatePresignedUploadUrlOutput> {
    try {
      const bucketName = this.configService.get<string>(
        'storage.bucketName',
        ''
      );
      if (!bucketName) {
        throw new Error('Missing S3 bucket name.');
      }

      const uploadForm = await createPresignedPost(this.s3Client, {
        Bucket: bucketName,
        Key: input.objectKey,
        Conditions: [
          ['content-length-range', 1, input.maxSizeBytes],
          ['eq', '$Content-Type', input.contentType]
        ],
        Fields: {
          'Content-Type': input.contentType
        },
        Expires: input.expiresInSeconds
      });

      return {
        method: 'POST',
        uploadUrl: uploadForm.url,
        fields: uploadForm.fields
      };
    } catch (error) {
      throw new StorageUploadUrlError(error);
    }
  }

  /**
   * Reads uploaded object metadata and a short byte signature.
   *
   * @param input - Uploaded object inspection request.
   * @returns Object metadata and detected MIME type.
   */
  async inspectUploadedObject(
    input: IInspectUploadedObjectInput
  ): Promise<IInspectUploadedObjectOutput> {
    const bucketName = this.configService.get<string>('storage.bucketName', '');

    try {
      if (!bucketName) {
        throw new Error('Missing S3 bucket name.');
      }

      const head = await this.s3Client.send(
        new HeadObjectCommand({
          Bucket: bucketName,
          Key: input.objectKey
        })
      );
      const bytes = await this.readSignatureBytes(bucketName, input.objectKey);

      return {
        contentType: head.ContentType ?? 'application/octet-stream',
        sizeBytes: head.ContentLength ?? 0,
        detectedContentType: this.detectImageContentType(bytes)
      };
    } catch (error) {
      if (this.isObjectNotFound(error)) {
        throw new StorageObjectNotFoundError(input.objectKey, error);
      }

      throw new StorageInspectionError(input.objectKey, error);
    }
  }

  /**
   * Creates a short-lived read URL for a private S3 object.
   *
   * @param input - Read URL request.
   * @returns Presigned read URL.
   */
  async createPresignedReadUrl(
    input: ICreatePresignedReadUrlInput
  ): Promise<ICreatePresignedReadUrlOutput> {
    const bucketName = this.configService.get<string>('storage.bucketName', '');

    try {
      if (!bucketName) {
        throw new Error('Missing S3 bucket name.');
      }

      const readUrl = await getSignedUrl(
        this.s3Client,
        new GetObjectCommand({
          Bucket: bucketName,
          Key: input.objectKey
        }),
        { expiresIn: input.expiresInSeconds }
      );

      return { readUrl };
    } catch (error) {
      throw new StorageInspectionError(input.objectKey, error);
    }
  }

  private async readSignatureBytes(
    bucketName: string,
    objectKey: string
  ): Promise<Uint8Array> {
    const object = await this.s3Client.send(
      new GetObjectCommand({
        Bucket: bucketName,
        Key: objectKey,
        Range: 'bytes=0-15'
      })
    );

    return this.bodyToBytes(object.Body);
  }

  private async bodyToBytes(
    body: GetObjectCommandOutput['Body']
  ): Promise<Uint8Array> {
    if (!body) {
      return new Uint8Array();
    }

    if (
      typeof body === 'object' &&
      'transformToByteArray' in body &&
      typeof body.transformToByteArray === 'function'
    ) {
      return body.transformToByteArray();
    }

    const chunks: Uint8Array[] = [];

    for await (const chunk of body as AsyncIterable<Buffer | Uint8Array | string>) {
      chunks.push(
        typeof chunk === 'string' ? Buffer.from(chunk) : new Uint8Array(chunk)
      );
    }

    return Buffer.concat(chunks);
  }

  private detectImageContentType(bytes: Uint8Array): string | null {
    if (bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) {
      return 'image/jpeg';
    }

    if (
      bytes[0] === 0x89 &&
      bytes[1] === 0x50 &&
      bytes[2] === 0x4e &&
      bytes[3] === 0x47 &&
      bytes[4] === 0x0d &&
      bytes[5] === 0x0a &&
      bytes[6] === 0x1a &&
      bytes[7] === 0x0a
    ) {
      return 'image/png';
    }

    if (
      bytes[0] === 0x52 &&
      bytes[1] === 0x49 &&
      bytes[2] === 0x46 &&
      bytes[3] === 0x46 &&
      bytes[8] === 0x57 &&
      bytes[9] === 0x45 &&
      bytes[10] === 0x42 &&
      bytes[11] === 0x50
    ) {
      return 'image/webp';
    }

    return null;
  }

  private isObjectNotFound(error: unknown): boolean {
    if (!error || typeof error !== 'object') {
      return false;
    }

    const candidate = error as {
      name?: unknown;
      $metadata?: { httpStatusCode?: number };
    };

    return (
      candidate.name === 'NotFound' ||
      candidate.name === 'NoSuchKey' ||
      candidate.$metadata?.httpStatusCode === 404
    );
  }
}
