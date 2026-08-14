import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import type { ITokenVerifierPort } from '@/modules/auth/application/ports/token-verifier.port';
import { ImageUploadEntity } from '@/modules/files/domain/entities/image-upload.entity';
import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import type { IImageUploadRepository } from '@/modules/files/domain/repositories/image-upload.repository';
import type { StorageObjectNotFoundError as StorageObjectNotFoundErrorClass } from '@/modules/files/infrastructure/errors/storage-object-not-found.error';
import type { IFileStoragePort } from '@/modules/files/application/ports/file-storage.port';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import {
  createActiveUser,
  createActiveUserRepository
} from '../support/active-user-repository.mock';

describe('HTTP API response contract', () => {
  const originalDatabaseUrl = process.env.DATABASE_URL;
  let app: NestFastifyApplication;
  let tokenVerifier: jest.Mocked<ITokenVerifierPort>;
  let fileStorage: jest.Mocked<IFileStoragePort>;
  let imageUploadRepository: jest.Mocked<IImageUploadRepository>;
  let users: ReturnType<typeof createActiveUserRepository>;
  let StorageObjectNotFoundError: typeof StorageObjectNotFoundErrorClass;

  beforeAll(async () => {
    delete process.env.DATABASE_URL;
    process.env.APP_S3_KEY_PREFIX = 'test/uploads';
    process.env.APP_S3_PROFILE_IMAGE_MAX_BYTES = '5242880';
    process.env.APP_S3_ALLOWED_IMAGE_MIME_TYPES =
      'image/jpeg,image/png,image/webp';

    jest.resetModules();
    const [
      { Test },
      { FastifyAdapter },
      { configureValidation },
      { AppModule },
      { TOKEN_VERIFIER_PORT },
      { IMAGE_UPLOAD_REPOSITORY },
      { FILE_STORAGE_PORT },
      { PrismaService },
      storageErrors,
      { USER_REPOSITORY },
      { ActiveUserAccountGuard }
    ] = await Promise.all([
        import('@nestjs/testing'),
        import('@nestjs/platform-fastify'),
        import('@/bootstrap/validation'),
        import('@/app.module'),
        import('@/modules/auth/application/ports/token-verifier.port'),
        import('@/modules/files/domain/repositories/image-upload.repository'),
        import('@/modules/files/application/ports/file-storage.port'),
        import('@/shared/infrastructure/database/prisma.service'),
        import('@/modules/files/infrastructure/errors/storage-object-not-found.error'),
        import('../../src/modules/users/domain/repositories/user.repository'),
        import('../../src/modules/users/interfaces/http/guards/active-user-account.guard')
      ]);
    StorageObjectNotFoundError = storageErrors.StorageObjectNotFoundError;
    tokenVerifier = {
      verify: jest.fn().mockResolvedValue({
        sub: 'cognito-sub',
        email: 'user@example.test',
        groups: []
      })
    };
    users = createActiveUserRepository({
      id: 'user-id',
      subject: 'cognito-sub',
      email: 'user@example.test'
    });
    fileStorage = {
      createPresignedUploadUrl: jest.fn().mockResolvedValue({
        method: 'POST',
        uploadUrl: 'https://upload.example.test',
        fields: {
          key: 'test/uploads/profile-image/cognito-sub/file.jpg',
          policy: 'policy'
        }
      }),
      inspectUploadedObject: jest.fn().mockResolvedValue({
        contentType: 'image/jpeg',
        sizeBytes: 512,
        detectedContentType: 'image/jpeg'
      }),
      promoteUploadedObject: jest.fn().mockResolvedValue(undefined),
      createPresignedReadUrl: jest.fn().mockResolvedValue({
        readUrl: 'https://read.example.test/file.jpg'
      })
    };
    imageUploadRepository = {
      findById: jest.fn().mockResolvedValue(null),
      saveConfirmedUpload: jest.fn().mockResolvedValue(
        ImageUploadEntity.fromPersistence({
          id: 'image-id',
          ownerSub: 'cognito-sub',
          ownerEmail: 'user@example.test',
          ownerName: null,
          ownerPictureUrl: null,
          ownerProvider: null,
          purpose: FilePurpose.ProfileImage,
          objectKey: 'test/uploads/profile-image/cognito-sub/2026/06/file.jpg',
          contentType: 'image/jpeg',
          sizeBytes: 512,
          createdAt: new Date('2026-06-11T00:00:00.000Z')
        })
      ),
      listRecent: jest.fn().mockResolvedValue([])
    };
    const moduleRef = await Test.createTestingModule({
      imports: [AppModule]
    })
      .overrideProvider(TOKEN_VERIFIER_PORT)
      .useValue(tokenVerifier)
      .overrideProvider(USER_REPOSITORY)
      .useValue(users)
      .overrideProvider(ActiveUserAccountGuard)
      .useValue({ canActivate: jest.fn().mockReturnValue(true) })
      .overrideProvider(PrismaService)
      .useValue({
        onModuleInit: jest.fn(),
        onModuleDestroy: jest.fn()
      })
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
    users.findByCognitoSub.mockResolvedValue(
      createActiveUser({
        id: 'user-id',
        subject: 'cognito-sub',
        email: 'user@example.test'
      })
    );
  });

  afterAll(async () => {
    await app?.close();

    if (originalDatabaseUrl) {
      process.env.DATABASE_URL = originalDatabaseUrl;
    } else {
      delete process.env.DATABASE_URL;
    }
  });

  it('wraps successful endpoint data in the global envelope', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/health'
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      success: true,
      data: {
        status: 'ok',
        timestamp: expect.any(String)
      },
      errors: [],
      meta: {
        requestId: expect.any(String),
        path: '/v1/health',
        timestamp: expect.any(String)
      }
    });
  });

  it('returns auth errors with the global error envelope', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/auth/me'
    });

    expect(response.statusCode).toBe(401);
    expect(response.json()).toEqual({
      success: false,
      data: null,
      errors: [
        {
          code: APP_ERROR_CODES.AUTH_MISSING_TOKEN,
          message: 'Authentication token is required.',
          status: 401,
          type: 'urn:problem-type:backend:auth-missing-token'
        }
      ],
      meta: {
        requestId: expect.any(String),
        path: '/v1/auth/me',
        timestamp: expect.any(String)
      }
    });
  });

  it('returns validation errors from DTO pipes with the global error envelope', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/files/uploads',
      headers: {
        Authorization: 'Bearer token'
      },
      payload: {
        purpose: FilePurpose.ProfileImage,
        contentType: 'not-a-mime-type',
        sizeBytes: 100
      }
    });

    expect(response.statusCode).toBe(400);
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            status: 400
          })
        ],
        meta: expect.objectContaining({
          path: '/v1/files/uploads'
        })
      })
    );
  });

  it('lists only the authenticated user uploads through the repository port', async () => {
    imageUploadRepository.listRecent.mockResolvedValueOnce([
      ImageUploadEntity.fromPersistence({
        id: 'image-id',
        ownerSub: 'cognito-sub',
        ownerEmail: 'user@example.test',
        ownerName: null,
        ownerPictureUrl: null,
        ownerProvider: null,
        purpose: FilePurpose.CourtImage,
        objectKey: 'test/uploads/court-image/cognito-sub/2026/06/file.jpg',
        contentType: 'image/jpeg',
        sizeBytes: 512,
        createdAt: new Date('2026-06-11T00:00:00.000Z')
      })
    ]);

    const response = await app.inject({
      method: 'GET',
      url: '/v1/files/uploads',
      headers: {
        Authorization: 'Bearer token'
      }
    });

    expect(response.statusCode).toBe(200);
    expect(imageUploadRepository.listRecent).toHaveBeenCalledWith('cognito-sub', 50);
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: true,
        data: [
          expect.objectContaining({
            id: 'image-id',
            purpose: FilePurpose.CourtImage,
            uploadedBy: expect.objectContaining({
              sub: 'cognito-sub'
            })
          })
        ]
      })
    );
  });

  it('maps domain errors to their HTTP status without controller catches', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/files/uploads',
      headers: {
        Authorization: 'Bearer token'
      },
      payload: {
        purpose: FilePurpose.ProfileImage,
        contentType: 'image/jpeg',
        sizeBytes: 6000000
      }
    });

    expect(response.statusCode).toBe(413);
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          {
            code: APP_ERROR_CODES.PAYLOAD_TOO_LARGE,
            message: 'File is too large.',
            status: 413,
            type: 'urn:problem-type:backend:payload-too-large'
          }
        ]
      })
    );
  });

  it('confirms uploaded images through the application and infrastructure ports', async () => {
    const objectKey = 'test/uploads/profile-image/cognito-sub/2026/06/file.jpg';
    const response = await app.inject({
      method: 'POST',
      url: '/v1/files/uploads/confirm',
      headers: {
        Authorization: 'Bearer token'
      },
      payload: {
        purpose: FilePurpose.ProfileImage,
        objectKey
      }
    });

    expect(response.statusCode).toBe(201);
    expect(response.json()).toEqual({
      success: true,
      data: {
        id: 'image-id',
        objectKey,
        purpose: FilePurpose.ProfileImage,
        status: 'ready',
        contentType: 'image/jpeg',
        sizeBytes: 512,
        readUrl: 'https://read.example.test/file.jpg',
        createdAt: '2026-06-11T00:00:00.000Z',
        uploadedBy: {
          sub: 'cognito-sub',
          email: 'user@example.test'
        }
      },
      errors: [],
      meta: {
        requestId: expect.any(String),
        path: '/v1/files/uploads/confirm',
        timestamp: expect.any(String)
      }
    });
    expect(fileStorage.inspectUploadedObject).toHaveBeenCalledWith({
      objectKey
    });
    expect(imageUploadRepository.saveConfirmedUpload).toHaveBeenCalledWith({
      ownerSub: 'cognito-sub',
      ownerEmail: 'user@example.test',
      ownerName: undefined,
      ownerPictureUrl: undefined,
      ownerProvider: undefined,
      purpose: FilePurpose.ProfileImage,
      objectKey:
        'test/uploads/confirmed/profile-image/cognito-sub/2026/06/file.jpg',
      contentType: 'image/jpeg',
      sizeBytes: 512
    });
  });

  it('rejects confirmation for files outside the current user prefix before storage access', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/files/uploads/confirm',
      headers: {
        Authorization: 'Bearer token'
      },
      payload: {
        purpose: FilePurpose.ProfileImage,
        objectKey: 'test/uploads/profile-image/other-sub/2026/06/file.jpg'
      }
    });

    expect(response.statusCode).toBe(403);
    expect(fileStorage.inspectUploadedObject).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.FORBIDDEN,
            status: 403
          })
        ]
      })
    );
  });

  it('returns storage not found errors from the global error envelope', async () => {
    const objectKey = 'test/uploads/profile-image/cognito-sub/2026/06/missing.jpg';
    fileStorage.inspectUploadedObject.mockRejectedValueOnce(
      new StorageObjectNotFoundError(objectKey)
    );

    const response = await app.inject({
      method: 'POST',
      url: '/v1/files/uploads/confirm',
      headers: {
        Authorization: 'Bearer token'
      },
      payload: {
        purpose: FilePurpose.ProfileImage,
        objectKey
      }
    });

    expect(response.statusCode).toBe(404);
    expect(response.json()).toEqual(
      expect.objectContaining({
        errors: [
          {
            code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
            message: 'Uploaded file was not found.',
            status: 404,
            type: 'urn:problem-type:backend:resource-not-found'
          }
        ]
      })
    );
  });
});
