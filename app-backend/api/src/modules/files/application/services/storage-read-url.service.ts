import { Inject, Injectable } from '@nestjs/common';
import type { IFileReadUrlPort } from '../ports/file-read-url.port';
import type { IFileStoragePort } from '../ports/file-storage.port';
import { FILE_STORAGE_PORT } from '../ports/file-storage.port';
import { READ_URL_TTL_SECONDS } from '../tokens/read-url-ttl-seconds.token';

@Injectable()
export class StorageReadUrlService implements IFileReadUrlPort {
  constructor(
    @Inject(FILE_STORAGE_PORT)
    private readonly fileStorage: IFileStoragePort,
    @Inject(READ_URL_TTL_SECONDS)
    private readonly readUrlTtlSeconds: number
  ) {}

  async createReadUrl(objectKey: string): Promise<string> {
    const read = await this.fileStorage.createPresignedReadUrl({
      objectKey,
      expiresInSeconds: this.readUrlTtlSeconds
    });

    return read.readUrl;
  }
}
