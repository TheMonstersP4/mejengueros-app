import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { FastifyAdapter } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';
import { configureValidation } from '@/bootstrap/validation';
import type { ITokenVerifierPort } from '@/modules/auth/application/ports/token-verifier.port';
import { TOKEN_VERIFIER_PORT } from '@/modules/auth/application/ports/token-verifier.port';
import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import { ImageUploadEntity } from '@/modules/files/domain/entities/image-upload.entity';
import type { IImageUploadRepository } from '@/modules/files/domain/repositories/image-upload.repository';
import { IMAGE_UPLOAD_REPOSITORY } from '@/modules/files/domain/repositories/image-upload.repository';
import type { IFileStoragePort } from '@/modules/files/application/ports/file-storage.port';
import { FILE_STORAGE_PORT } from '@/modules/files/application/ports/file-storage.port';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

describe('files HTTP contract', () => {
  const originalEnv = {
    DATABASE_URL: process.env.DATABASE_URL,
    AWS_REGION: process.env.AWS_REGION,
    COGNITO_USER_POOL_ID: process.env.COGNITO_USER_POOL_ID,
    COGNITO_CLIENT_ID: process.env.COGNITO_CLIENT_ID,
    APP_S3_BUCKET_NAME: process.env.APP_S3_BUCKET_NAME,
    APP_S3_KEY_PREFIX: process.env.APP_S3_KEY_PREFIX,
    APP_S3_PROFILE_IMAGE_MAX_BYTES: process.env.APP_S3_PROFILE_IMAGE_MAX_BYTES,
    APP_S3_ALLOWED_IMAGE_MIME_TYPES: process.env.APP_S3_ALLOWED_IMAGE_MIME_TYPES,
    WEBSOCKET_CONNECTIONS_TABLE_NAME: process.env.WEBSOCKET_CONNECTIONS_TABLE_NAME
  };

  let app: NestFastifyApplication;
  let tokenVerifier: jest.Mocked<ITokenVerifierPort>;
  let fileStorage: jest.Mocked<IFileStoragePort>;
  let imageUploadRepository: jest.Mocked<IImageUploadRepository>;

  beforeAll(async () => {
    process.env.DATABASE_URL = 'file:memory:';
    process.env.AWS_REGION = 'us-east-1';
    process.env.COGNITO_USER_POOL_ID = 'us-east-1_test';
    process.env.COGNITO_CLIENT_ID = 'test-client-id';
    process.env.APP_S3_BUCKET_NAME = 'test-bucket';
    process.env.APP_S3_KEY_PREFIX = 'test/uploads';
    process.env.APP_S3_PROFILE_IMAGE_MAX_BYTES = '5242880';
    process.env.APP_S3_ALLOWED_IMAGE_MIME_TYPES = 'image/jpeg,image/png,image/webp';
    process.env.WEBSOCKET_CONNECTIONS_TABLE_NAME = 'test-websocket-connections';

    const { AppModule } = await import('@/app.module');

    tokenVerifier = {
      verify: jest.fn().mockResolvedValue({
        sub: 'player-sub',
        email: 'player@example.test',
        emailVerified: true,
        name: 'Player One',
        pictureUrl: 'https://example.test/player.png',
        provider: 'Google',
        groups: ['players']
      })
    };
    fileStorage = {
      createPresignedUploadUrl: jest.fn().mockResolvedValue({
        method: 'POST',
        uploadUrl: 'https://upload.example.test',
        fields: {
          key: 'test/uploads/review-evidence-image/player-sub/2026/07/evidence.jpg',
          policy: 'policy'
        }
      }),
      inspectUploadedObject: jest.fn().mockResolvedValue({
        contentType: 'image/jpeg',
        sizeBytes: 512,
        detectedContentType: 'image/jpeg'
      }),
      createPresignedReadUrl: jest.fn().mockResolvedValue({
        readUrl: 'https://read.example.test/evidence.jpg'
      })
    };
    imageUploadRepository = {
      findById: jest.fn().mockResolvedValue(null),
      saveConfirmedUpload: jest.fn().mockResolvedValue(createReviewEvidenceUpload()),
      listRecent: jest.fn().mockResolvedValue([])
    };

    const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(PrismaService)
      .useValue({
        onModuleInit: jest.fn(),
        onModuleDestroy: jest.fn()
      })
      .overrideProvider(TOKEN_VERIFIER_PORT)
      .useValue(tokenVerifier)
      .overrideProvider(FILE_STORAGE_PORT)
      .useValue(fileStorage)
      .overrideProvider(IMAGE_UPLOAD_REPOSITORY)
      .useValue(imageUploadRepository)
      .compile();

    app = moduleRef.createNestApplication<NestFastifyApplication>(
      new FastifyAdapter({ logger: false })
    );
    app.setGlobalPrefix('v1');
    configureValidation(app);
    await app.init();
    await app.getHttpAdapter().getInstance().ready();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    tokenVerifier.verify.mockResolvedValue({
      sub: 'player-sub',
      email: 'player@example.test',
      emailVerified: true,
      name: 'Player One',
      pictureUrl: 'https://example.test/player.png',
      provider: 'Google',
      groups: ['players']
    });
    fileStorage.createPresignedUploadUrl.mockResolvedValue({
      method: 'POST',
      uploadUrl: 'https://upload.example.test',
      fields: {
        key: 'test/uploads/review-evidence-image/player-sub/2026/07/evidence.jpg',
        policy: 'policy'
      }
    });
    fileStorage.inspectUploadedObject.mockResolvedValue({
      contentType: 'image/jpeg',
      sizeBytes: 512,
      detectedContentType: 'image/jpeg'
    });
    fileStorage.createPresignedReadUrl.mockResolvedValue({
      readUrl: 'https://read.example.test/evidence.jpg'
    });
    imageUploadRepository.saveConfirmedUpload.mockResolvedValue(createReviewEvidenceUpload());
  });

  afterAll(async () => {
    await app?.close();

    for (const [key, value] of Object.entries(originalEnv)) {
      if (value === undefined) {
        delete process.env[key];
      } else {
        process.env[key] = value;
      }
    }
  });

  it('creates upload URLs for review evidence images through the guarded HTTP boundary', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/files/uploads',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        purpose: FilePurpose.ReviewEvidenceImage,
        contentType: 'image/jpeg',
        sizeBytes: 512
      }
    });

    expect(response.statusCode).toBe(201);
    expect(fileStorage.createPresignedUploadUrl).toHaveBeenCalledWith(
      expect.objectContaining({
        contentType: 'image/jpeg',
        maxSizeBytes: 5242880
      })
    );
    expect(response.json()).toEqual({
      success: true,
      data: expect.objectContaining({
        objectKey: expect.stringContaining('/review-evidence-image/player-sub/'),
        method: 'POST',
        uploadUrl: 'https://upload.example.test',
        maxSizeBytes: 5242880
      }),
      errors: [],
      meta: expect.objectContaining({ path: '/v1/files/uploads' })
    });
  });

  it('rejects malformed upload URL purposes before touching storage', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/files/uploads',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        purpose: 'banner-image',
        contentType: 'image/jpeg',
        sizeBytes: 512
      }
    });

    expect(response.statusCode).toBe(400);
    expect(fileStorage.createPresignedUploadUrl).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            status: 400,
            message: expect.stringContaining('purpose must be one of the following values')
          })
        ],
        meta: expect.objectContaining({ path: '/v1/files/uploads' })
      })
    );
  });

  it('confirms review evidence images through the guarded HTTP boundary', async () => {
    const objectKey = 'test/uploads/review-evidence-image/player-sub/2026/07/evidence.jpg';

    const response = await app.inject({
      method: 'POST',
      url: '/v1/files/uploads/confirm',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        purpose: FilePurpose.ReviewEvidenceImage,
        objectKey
      }
    });

    expect(response.statusCode).toBe(201);
    expect(fileStorage.inspectUploadedObject).toHaveBeenCalledWith({ objectKey });
    expect(imageUploadRepository.saveConfirmedUpload).toHaveBeenCalledWith({
      ownerSub: 'player-sub',
      ownerEmail: 'player@example.test',
      ownerName: 'Player One',
      ownerPictureUrl: 'https://example.test/player.png',
      ownerProvider: 'Google',
      purpose: FilePurpose.ReviewEvidenceImage,
      objectKey,
      contentType: 'image/jpeg',
      sizeBytes: 512
    });
    expect(response.json()).toEqual({
      success: true,
      data: expect.objectContaining({
        id: 'review-evidence-upload-id',
        objectKey,
        purpose: FilePurpose.ReviewEvidenceImage,
        readUrl: 'https://read.example.test/evidence.jpg'
      }),
      errors: [],
      meta: expect.objectContaining({ path: '/v1/files/uploads/confirm' })
    });
  });

  it('rejects review evidence confirmations for another owner before storage access', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/files/uploads/confirm',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        purpose: FilePurpose.ReviewEvidenceImage,
        objectKey: 'test/uploads/review-evidence-image/other-sub/2026/07/evidence.jpg'
      }
    });

    expect(response.statusCode).toBe(403);
    expect(fileStorage.inspectUploadedObject).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.FORBIDDEN,
            status: 403
          })
        ],
        meta: expect.objectContaining({ path: '/v1/files/uploads/confirm' })
      })
    );
  });
});

function createReviewEvidenceUpload(): ImageUploadEntity {
  return ImageUploadEntity.fromPersistence({
    id: 'review-evidence-upload-id',
    ownerSub: 'player-sub',
    ownerEmail: 'player@example.test',
    ownerName: 'Player One',
    ownerPictureUrl: 'https://example.test/player.png',
    ownerProvider: 'Google',
    purpose: FilePurpose.ReviewEvidenceImage,
    objectKey: 'test/uploads/review-evidence-image/player-sub/2026/07/evidence.jpg',
    contentType: 'image/jpeg',
    sizeBytes: 512,
    createdAt: new Date('2026-07-03T01:30:00.000Z')
  });
}
