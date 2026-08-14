import { FastifyAdapter } from '@nestjs/platform-fastify';
import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';
import { configureValidation } from '@/bootstrap/validation';
import type { ITokenVerifierPort } from '@/modules/auth/application/ports/token-verifier.port';
import { TOKEN_VERIFIER_PORT } from '@/modules/auth/application/ports/token-verifier.port';
import type { IFileReadUrlPort } from '@/modules/files/application/ports/file-read-url.port';
import { FILE_READ_URL_PORT } from '@/modules/files/application/ports/file-read-url.port';
import { ImageUploadEntity } from '@/modules/files/domain/entities/image-upload.entity';
import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import type { IImageUploadRepository } from '@/modules/files/domain/repositories/image-upload.repository';
import { IMAGE_UPLOAD_REPOSITORY } from '@/modules/files/domain/repositories/image-upload.repository';
import { UserEntity } from '@/modules/users/domain/entities/user.entity';
import type { IUserRepository } from '@/modules/users/domain/repositories/user.repository';
import { USER_REPOSITORY } from '@/modules/users/domain/repositories/user.repository';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

describe('users profile image HTTP contract', () => {
  const imageUploadId = 'd55d773e-d6e9-4f67-aa1a-87f64dfd79c2';
  const originalDatabaseUrl = process.env.DATABASE_URL;
  let app: NestFastifyApplication;
  let users: jest.Mocked<IUserRepository>;
  let images: jest.Mocked<IImageUploadRepository>;

  beforeAll(async () => {
    process.env.DATABASE_URL = 'postgresql://test:test@localhost:5432/test';
    process.env.AWS_REGION = 'us-east-1';
    process.env.COGNITO_USER_POOL_ID = 'us-east-1_test';
    process.env.COGNITO_CLIENT_ID = 'test-client-id';
    process.env.APP_S3_BUCKET_NAME = 'test-bucket';

    const { AppModule } = await import('@/app.module');
    const tokenVerifier: jest.Mocked<ITokenVerifierPort> = {
      verify: jest.fn().mockResolvedValue({
        sub: 'player-sub',
        email: 'player@example.test',
        pictureUrl: 'https://provider.example.test/avatar.png',
        groups: ['players']
      })
    };
    users = {
      syncAuthenticatedUser: jest.fn().mockResolvedValue(createUser()),
      findByCognitoSub: jest.fn(),
      replaceProfileImage: jest
        .fn()
        .mockResolvedValue(createUser(imageUploadId)),
      updateAccount: jest.fn(),
      list: jest.fn(),
      deactivateById: jest.fn()
    };
    images = {
      findById: jest.fn().mockResolvedValue(createUpload()),
      saveConfirmedUpload: jest.fn(),
      listRecent: jest.fn()
    };
    const readUrls: jest.Mocked<IFileReadUrlPort> = {
      createReadUrl: jest
        .fn()
        .mockResolvedValue('https://read.example.test/profile.jpg')
    };

    const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(PrismaService)
      .useValue({ onModuleInit: jest.fn(), onModuleDestroy: jest.fn() })
      .overrideProvider(TOKEN_VERIFIER_PORT)
      .useValue(tokenVerifier)
      .overrideProvider(USER_REPOSITORY)
      .useValue(users)
      .overrideProvider(IMAGE_UPLOAD_REPOSITORY)
      .useValue(images)
      .overrideProvider(FILE_READ_URL_PORT)
      .useValue(readUrls)
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
    users.syncAuthenticatedUser.mockResolvedValue(createUser());
    users.replaceProfileImage.mockResolvedValue(createUser(imageUploadId));
    images.findById.mockResolvedValue(createUpload());
  });

  afterAll(async () => {
    await app?.close();

    if (originalDatabaseUrl === undefined) {
      delete process.env.DATABASE_URL;
    } else {
      process.env.DATABASE_URL = originalDatabaseUrl;
    }
  });

  it('replaces the current user profile image without returning an object key', async () => {
    const response = await app.inject({
      method: 'PUT',
      url: '/v1/users/me/profile-image',
      headers: { Authorization: 'Bearer valid-token' },
      payload: { imageUploadId }
    });

    expect(response.statusCode).toBe(200);
    expect(users.replaceProfileImage).toHaveBeenCalledWith({
      userId: 'user-id',
      imageUploadId
    });
    expect(response.json()).toEqual({
      success: true,
      data: expect.objectContaining({
        id: 'user-id',
        pictureUrl: 'https://read.example.test/profile.jpg'
      }),
      errors: [],
      meta: expect.objectContaining({ path: '/v1/users/me/profile-image' })
    });
    expect(response.json().data).not.toHaveProperty('profileImageUploadId');
    expect(response.json().data).not.toHaveProperty('objectKey');
  });

  it('rejects malformed upload UUIDs before invoking the use case', async () => {
    const response = await app.inject({
      method: 'PUT',
      url: '/v1/users/me/profile-image',
      headers: { Authorization: 'Bearer valid-token' },
      payload: { imageUploadId: 'not-a-uuid' }
    });

    expect(response.statusCode).toBe(400);
    expect(users.syncAuthenticatedUser).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({ code: APP_ERROR_CODES.VALIDATION_FAILED })
        ]
      })
    );
  });

  it('maps missing confirmed uploads to RFC 9457 not found errors', async () => {
    images.findById.mockResolvedValue(null);

    const response = await app.inject({
      method: 'PUT',
      url: '/v1/users/me/profile-image',
      headers: { Authorization: 'Bearer valid-token' },
      payload: { imageUploadId }
    });

    expect(response.statusCode).toBe(404);
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
            status: 404,
            type: 'urn:problem-type:backend:resource-not-found'
          })
        ]
      })
    );
    expect(users.replaceProfileImage).not.toHaveBeenCalled();
  });

  it('returns the provider picture fallback from the current profile endpoint', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/users/me',
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(response.json().data.pictureUrl).toBe(
      'https://provider.example.test/avatar.png'
    );
    expect(images.findById).not.toHaveBeenCalled();
  });

  it('returns a fresh custom signed URL from the current profile endpoint', async () => {
    users.syncAuthenticatedUser.mockResolvedValue(createUser(imageUploadId));

    const response = await app.inject({
      method: 'GET',
      url: '/v1/users/me',
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(response.json().data.pictureUrl).toBe(
      'https://read.example.test/profile.jpg'
    );
    expect(images.findById).toHaveBeenCalledWith(imageUploadId);
  });
});

function createUser(profileImageUploadId?: string): UserEntity {
  return UserEntity.fromPersistence({
    id: 'user-id',
    email: 'player@example.test',
    pictureUrl: 'https://provider.example.test/avatar.png',
    profileImageUploadId,
    status: 'ACTIVE',
    currentIdentity: { provider: 'Google', providerSubject: 'player-sub' },
    roles: ['PLAYER']
  });
}

function createUpload(): ImageUploadEntity {
  return ImageUploadEntity.fromPersistence({
    id: 'd55d773e-d6e9-4f67-aa1a-87f64dfd79c2',
    ownerSub: 'player-sub',
    purpose: FilePurpose.ProfileImage,
    objectKey:
      'test/uploads/confirmed/profile-image/player-sub/2026/07/profile.jpg',
    contentType: 'image/jpeg',
    sizeBytes: 512,
    createdAt: new Date('2026-07-29T18:00:00.000Z')
  });
}
