import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { PrismaModule } from '../../shared/infrastructure/database/prisma.module';
import { S3StorageModule } from '../../shared/infrastructure/storage/s3-storage.module';
import { AuthModule } from '../auth/auth.module';
import { FILE_STORAGE_PORT } from './application/ports/file-storage.port';
import { READ_URL_TTL_SECONDS } from './application/tokens/read-url-ttl-seconds.token';
import { UPLOAD_URL_TTL_SECONDS } from './application/tokens/upload-url-ttl-seconds.token';
import { ConfirmUploadUseCase } from './application/use-cases/confirm-upload.use-case';
import { CreateUploadUrlUseCase } from './application/use-cases/create-upload-url.use-case';
import { ListImageUploadsUseCase } from './application/use-cases/list-image-uploads.use-case';
import { IMAGE_UPLOAD_REPOSITORY } from './domain/repositories/image-upload.repository';
import { ImageUploadPolicyService } from './domain/services/image-upload-policy.service';
import { DisabledImageUploadRepository } from './infrastructure/persistence/disabled-image-upload.repository';
import { PrismaImageUploadRepository } from './infrastructure/persistence/prisma-image-upload.repository';
import { S3FileStorageAdapter } from './infrastructure/storage/s3-file-storage.adapter';
import { FilesController } from './interfaces/http/controllers/files.controller';

const databaseImports = process.env.DATABASE_URL ? [PrismaModule] : [];
const imageUploadRepositoryClass = process.env.DATABASE_URL
  ? PrismaImageUploadRepository
  : DisabledImageUploadRepository;

/**
 * Feature module for application-managed files.
 */
@Module({
  imports: [AuthModule, ConfigModule, ...databaseImports, S3StorageModule],
  controllers: [FilesController],
  providers: [
    ConfirmUploadUseCase,
    CreateUploadUrlUseCase,
    ListImageUploadsUseCase,
    {
      provide: ImageUploadPolicyService,
      inject: [ConfigService],
      useFactory: (configService: ConfigService): ImageUploadPolicyService =>
        new ImageUploadPolicyService({
          allowedMimeTypes: configService.get<string[]>(
            'storage.allowedImageMimeTypes',
            ['image/jpeg', 'image/png', 'image/webp']
          ),
          profileImageMaxBytes: configService.get<number>(
            'storage.profileImageMaxBytes',
            5242880
          ),
          keyPrefix: configService.get<string>('storage.keyPrefix', 'uploads')
        })
    },
    {
      provide: UPLOAD_URL_TTL_SECONDS,
      inject: [ConfigService],
      useFactory: (configService: ConfigService): number =>
        configService.get<number>('storage.uploadUrlTtlSeconds', 300)
    },
    {
      provide: READ_URL_TTL_SECONDS,
      inject: [ConfigService],
      useFactory: (configService: ConfigService): number =>
        configService.get<number>('storage.uploadUrlTtlSeconds', 300)
    },
    {
      provide: IMAGE_UPLOAD_REPOSITORY,
      useClass: imageUploadRepositoryClass
    },
    {
      provide: FILE_STORAGE_PORT,
      useClass: S3FileStorageAdapter
    }
  ]
})
export class FilesModule {}
