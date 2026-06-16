import { S3Client } from '@aws-sdk/client-s3';
import type { ConfigService } from '@nestjs/config';
import { s3ClientFactory } from '@/shared/infrastructure/storage/s3-client.provider';

describe('s3ClientFactory', () => {
  it('creates an S3 client from storage region config', () => {
    const config = {
      get: jest.fn().mockReturnValue('us-west-2')
    } as unknown as ConfigService;

    expect(s3ClientFactory(config)).toBeInstanceOf(S3Client);
    expect(config.get).toHaveBeenCalledWith('storage.region', 'us-east-2');
  });
});
