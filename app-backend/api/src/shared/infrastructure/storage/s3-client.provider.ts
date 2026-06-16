import { S3Client } from '@aws-sdk/client-s3';
import type { ConfigService } from '@nestjs/config';

/**
 * Injection token for the shared S3 client.
 */
export const S3_CLIENT = Symbol('S3_CLIENT');

/**
 * Builds the shared S3 client used by storage adapters.
 *
 * @param configService - Application configuration reader.
 * @returns AWS S3 client configured for the application bucket region.
 */
export function s3ClientFactory(configService: ConfigService): S3Client {
  return new S3Client({
    region: configService.get<string>('storage.region', 'us-east-2')
  });
}
