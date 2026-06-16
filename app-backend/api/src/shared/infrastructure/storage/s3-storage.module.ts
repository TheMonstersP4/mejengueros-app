import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { S3_CLIENT, s3ClientFactory } from './s3-client.provider';

/**
 * Shared infrastructure module for AWS S3 access.
 */
@Module({
  imports: [ConfigModule],
  providers: [
    {
      provide: S3_CLIENT,
      inject: [ConfigService],
      useFactory: s3ClientFactory
    }
  ],
  exports: [S3_CLIENT]
})
export class S3StorageModule {}
