import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { PrismaService } from '../../shared/infrastructure/database/prisma.service';
import { S3StorageModule } from '../../shared/infrastructure/storage/s3-storage.module';
import { AuthModule } from '../auth/auth.module';
import {
  USER_REPOSITORY,
  type IExternalUserIdentity,
  type IReplaceUserProfileImageInput,
  type IUserRepository
} from '../users/domain/repositories/user.repository';
import type { PrismaUserRepository as PrismaUserRepositoryClass } from '../users/infrastructure/persistence/prisma-user.repository';
import { ActiveUserAccountGuard } from '../users/interfaces/http/guards/active-user-account.guard';
import { FILE_READ_URL_PORT } from './application/ports/file-read-url.port';
import { FILE_STORAGE_PORT } from './application/ports/file-storage.port';
import { StorageReadUrlService } from './application/services/storage-read-url.service';
import { READ_URL_TTL_SECONDS } from './application/tokens/read-url-ttl-seconds.token';
import { UPLOAD_URL_TTL_SECONDS } from './application/tokens/upload-url-ttl-seconds.token';
import { ConfirmUploadUseCase } from './application/use-cases/confirm-upload.use-case';
import { CreateUploadUrlUseCase } from './application/use-cases/create-upload-url.use-case';
import { ListImageUploadsUseCase } from './application/use-cases/list-image-uploads.use-case';
import { PROFILE_IMAGE_DEFAULT_MAX_BYTES } from './domain/constants/image-upload.constants';
import {
  IMAGE_UPLOAD_REPOSITORY,
  type IImageUploadRepository
} from './domain/repositories/image-upload.repository';
import { ImageUploadPolicyService } from './domain/services/image-upload-policy.service';
import { DisabledImageUploadRepository } from './infrastructure/persistence/disabled-image-upload.repository';
import type { PrismaImageUploadRepository as PrismaImageUploadRepositoryClass } from './infrastructure/persistence/prisma-image-upload.repository';
import { S3FileStorageAdapter } from './infrastructure/storage/s3-file-storage.adapter';
import { FilesController } from './interfaces/http/controllers/files.controller';

function loadPrismaUserRepository(): typeof PrismaUserRepositoryClass {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const { PrismaUserRepository } = require('../users/infrastructure/persistence/prisma-user.repository') as {
    PrismaUserRepository: typeof PrismaUserRepositoryClass;
  };

  return PrismaUserRepository;
}

function createImageUploadRepository(
  prisma?: PrismaService
): IImageUploadRepository {
  if (!prisma) {
    return new DisabledImageUploadRepository();
  }

  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const { PrismaImageUploadRepository } = require('./infrastructure/persistence/prisma-image-upload.repository') as {
    PrismaImageUploadRepository: typeof PrismaImageUploadRepositoryClass;
  };

  return new PrismaImageUploadRepository(prisma);
}

function createUserRepository(prisma?: PrismaService): IUserRepository {
  if (!prisma) {
    return new DisabledUserRepository();
  }

  const PrismaUserRepository = loadPrismaUserRepository();

  return new PrismaUserRepository(prisma);
}

class DisabledUserRepository implements IUserRepository {
  syncAuthenticatedUser(identity: IExternalUserIdentity): Promise<never> {
    void identity;
    throw new Error('User persistence is unavailable without DATABASE_URL.');
  }

  findByCognitoSub(cognitoSub: string): Promise<null> {
    void cognitoSub;
    return Promise.resolve(null);
  }

  replaceProfileImage(input: IReplaceUserProfileImageInput): Promise<never> {
    void input;
    throw new Error('User persistence is unavailable without DATABASE_URL.');
  }

  list(): Promise<never> {
    throw new Error('User persistence is unavailable without DATABASE_URL.');
  }

  deactivateById(userId: string): Promise<never> {
    void userId;
    throw new Error('User persistence is unavailable without DATABASE_URL.');
  }
}

/**
 * Feature module for application-managed files.
 */
@Module({
  imports: [AuthModule, ConfigModule, S3StorageModule],
  controllers: [FilesController],
  providers: [
    ConfirmUploadUseCase,
    CreateUploadUrlUseCase,
    ListImageUploadsUseCase,
    StorageReadUrlService,
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
            PROFILE_IMAGE_DEFAULT_MAX_BYTES
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
      inject: [{ token: PrismaService, optional: true }],
      useFactory: createImageUploadRepository
    },
    {
      provide: FILE_STORAGE_PORT,
      useClass: S3FileStorageAdapter
    },
    {
      provide: FILE_READ_URL_PORT,
      useExisting: StorageReadUrlService
    },
    {
      provide: USER_REPOSITORY,
      inject: [{ token: PrismaService, optional: true }],
      useFactory: createUserRepository
    },
    ActiveUserAccountGuard
  ],
  exports: [IMAGE_UPLOAD_REPOSITORY, FILE_READ_URL_PORT]
})
export class FilesModule {}
