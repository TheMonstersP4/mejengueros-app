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
import type { IFileReadUrlPort } from '@/modules/files/application/ports/file-read-url.port';
import { FILE_READ_URL_PORT } from '@/modules/files/application/ports/file-read-url.port';
import { SyncAuthenticatedUserUseCase } from '@/modules/users/application/use-cases/sync-authenticated-user.use-case';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

const reservationId = '38fad3d5-0f6a-4c8a-a49a-c3dce07af6cf';
const evidenceImageUploadId = '6f554321-6df0-43c4-b310-f3d7e6bf00a1';
const courtId = '0dd3a274-7d7b-45c6-a90d-4d14298ae7aa';
const TEST_DATABASE_URL = 'file:memory:';

describe('reviews HTTP contract', () => {
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
  let prismaService: ReturnType<typeof createPrismaMock>;
  let tokenVerifier: jest.Mocked<ITokenVerifierPort>;
  let syncAuthenticatedUser: jest.Mocked<SyncAuthenticatedUserUseCase>;
  let fileReadUrl: jest.Mocked<IFileReadUrlPort>;
  let imageUploadRepository: jest.Mocked<IImageUploadRepository>;

  beforeAll(async () => {
    process.env.DATABASE_URL = TEST_DATABASE_URL;
    process.env.AWS_REGION = 'us-east-1';
    process.env.COGNITO_USER_POOL_ID = 'us-east-1_test';
    process.env.COGNITO_CLIENT_ID = 'test-client-id';
    process.env.APP_S3_BUCKET_NAME = 'test-bucket';
    process.env.APP_S3_KEY_PREFIX = 'test/uploads';
    process.env.APP_S3_PROFILE_IMAGE_MAX_BYTES = '5242880';
    process.env.APP_S3_ALLOWED_IMAGE_MIME_TYPES = 'image/jpeg,image/png,image/webp';
    process.env.WEBSOCKET_CONNECTIONS_TABLE_NAME = 'test-websocket-connections';

    const { AppModule } = await import('@/app.module');

    prismaService = createPrismaMock();
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
    syncAuthenticatedUser = {
      execute: jest.fn().mockResolvedValue({
        id: 'user-id',
        email: 'player@example.test',
        roles: []
      })
    } as unknown as jest.Mocked<SyncAuthenticatedUserUseCase>;
    fileReadUrl = {
      createReadUrl: jest.fn().mockResolvedValue('https://read.example.test/court-a.png')
    } as jest.Mocked<IFileReadUrlPort>;
    imageUploadRepository = createImageUploadRepositoryMock();

    const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(PrismaService)
      .useValue(prismaService)
      .overrideProvider(TOKEN_VERIFIER_PORT)
      .useValue(tokenVerifier)
      .overrideProvider(SyncAuthenticatedUserUseCase)
      .useValue(syncAuthenticatedUser)
      .overrideProvider(FILE_READ_URL_PORT)
      .useValue(fileReadUrl)
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
    prismaService = Object.assign(prismaService, createPrismaMock());
    tokenVerifier.verify.mockResolvedValue({
      sub: 'player-sub',
      email: 'player@example.test',
      emailVerified: true,
      name: 'Player One',
      pictureUrl: 'https://example.test/player.png',
      provider: 'Google',
      groups: ['players']
    });
    syncAuthenticatedUser.execute.mockResolvedValue({
      id: 'user-id',
      email: 'player@example.test',
      roles: []
    });
    fileReadUrl.createReadUrl.mockResolvedValue('https://read.example.test/court-a.png');
    imageUploadRepository.findById.mockResolvedValue(createEvidenceImageUpload());
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

  it('returns the latest eligible reservation in the standard envelope', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/reviews/latest-eligible-reservation',
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(prismaService.reservation.findFirst).toHaveBeenCalledWith({
      where: {
        userId: 'user-id',
        status: 'COMPLETED',
        completedAt: { not: null },
        review: null,
        court: {
          deletedAt: null,
          complex: { deletedAt: null }
        }
      },
      orderBy: [{ completedAt: 'desc' }, { startsAt: 'desc' }],
      select: {
        id: true,
        startsAt: true,
        endsAt: true,
        court: {
          select: {
            name: true,
            imageUpload: {
              select: { objectKey: true }
            },
            complex: {
              select: { name: true }
            }
          }
        }
      }
    });
    expect(fileReadUrl.createReadUrl).toHaveBeenCalledWith(
      'uploads/court-image/player-sub/2026/07/court-a.png'
    );
    expect(response.json()).toEqual({
      success: true,
      data: {
        reservationId,
        complexName: 'Moravia FC',
        courtName: 'Cancha A',
        startsAt: '2026-07-02T20:00:00.000Z',
        endsAt: '2026-07-02T21:00:00.000Z',
        imageUrl: 'https://read.example.test/court-a.png'
      },
      errors: [],
      meta: expect.objectContaining({
        path: '/v1/reviews/latest-eligible-reservation'
      })
    });
  });

  it('returns the latest eligible reservation even when image URL signing fails', async () => {
    fileReadUrl.createReadUrl.mockRejectedValueOnce(new Error('signed URL unavailable'));

    const response = await app.inject({
      method: 'GET',
      url: '/v1/reviews/latest-eligible-reservation',
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      success: true,
      data: {
        reservationId,
        complexName: 'Moravia FC',
        courtName: 'Cancha A',
        startsAt: '2026-07-02T20:00:00.000Z',
        endsAt: '2026-07-02T21:00:00.000Z'
      },
      errors: [],
      meta: expect.objectContaining({
        path: '/v1/reviews/latest-eligible-reservation'
      })
    });
  });

  it('returns a success envelope with null data when no eligible reservation exists', async () => {
    prismaService.reservation.findFirst.mockResolvedValueOnce(null);

    const response = await app.inject({
      method: 'GET',
      url: '/v1/reviews/latest-eligible-reservation',
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(fileReadUrl.createReadUrl).not.toHaveBeenCalled();
    expect(response.json()).toEqual({
      success: true,
      data: null,
      errors: [],
      meta: expect.objectContaining({
        path: '/v1/reviews/latest-eligible-reservation'
      })
    });
  });

  it('creates a 1-star review through the guarded HTTP boundary', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/reviews',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        reservationId,
        rating: 1,
        comment: '  The court lights failed during the entire reservation.  ',
        evidenceImageUploadId
      }
    });

    expect(response.statusCode).toBe(201);
    expect(imageUploadRepository.findById).toHaveBeenCalledWith(evidenceImageUploadId);
    expect(prismaService.review.findFirst).toHaveBeenCalledWith({
      where: { evidenceImageUploadId },
      select: { id: true }
    });
    expect(prismaService.review.create).toHaveBeenCalledWith({
      data: {
        reservationId,
        rating: 1,
        comment: 'The court lights failed during the entire reservation.',
        evidenceImageUploadId
      }
    });
    expect(response.json()).toEqual({
      success: true,
      data: {
        id: 'review-id',
        reservationId,
        rating: 1,
        comment: 'The court lights failed during the entire reservation.',
        evidenceImageUploadId,
        createdAt: '2026-07-03T02:00:00.000Z'
      },
      errors: [],
      meta: expect.objectContaining({ path: '/v1/reviews' })
    });
  });

  it.each([
    {
      name: 'rejects unauthenticated latest-eligible reservation access',
      method: 'GET' as const,
      url: '/v1/reviews/latest-eligible-reservation',
      path: '/v1/reviews/latest-eligible-reservation'
    },
    {
      name: 'rejects unauthenticated review creation',
      method: 'POST' as const,
      url: '/v1/reviews',
      path: '/v1/reviews',
      payload: { reservationId, rating: 5 }
    }
  ])('$name', async ({ method, url, path, payload }) => {
    const response = await app.inject({ method, url, payload });

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
        path,
        timestamp: expect.any(String)
      }
    });
  });

  it('rejects malformed review payloads before touching persistence', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/reviews',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        reservationId,
        rating: 6,
        evidenceImageUploadId: 'not-a-uuid'
      }
    });

    expect(response.statusCode).toBe(400);
    expect(syncAuthenticatedUser.execute).not.toHaveBeenCalled();
    expect(prismaService.reservation.findUnique).not.toHaveBeenCalled();
    expect(imageUploadRepository.findById).not.toHaveBeenCalled();
    expect(prismaService.review.create).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            status: 400,
            message: 'rating must not be greater than 5',
            details: expect.arrayContaining([
              'rating must not be greater than 5',
              'evidenceImageUploadId must be a UUID'
            ])
          }),
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            status: 400,
            message: 'evidenceImageUploadId must be a UUID',
            details: expect.arrayContaining([
              'rating must not be greater than 5',
              'evidenceImageUploadId must be a UUID'
            ])
          })
        ],
        meta: expect.objectContaining({ path: '/v1/reviews' })
      })
    );
  });

  it('rejects 1-star reviews without evidence through the HTTP boundary', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/reviews',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        reservationId,
        rating: 1,
        comment: 'The field was unsafe.'
      }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.review.create).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            status: 400,
            message:
              'Las reseñas de 1 estrella deben incluir al menos una imagen como evidencia.'
          })
        ],
        meta: expect.objectContaining({ path: '/v1/reviews' })
      })
    );
  });

  it('rejects already assigned evidence uploads before creating the review', async () => {
    prismaService.review.findFirst.mockResolvedValueOnce({ id: 'existing-review-id' });

    const response = await app.inject({
      method: 'POST',
      url: '/v1/reviews',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        reservationId,
        rating: 1,
        comment: 'The field was unsafe.',
        evidenceImageUploadId
      }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.review.create).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            status: 400,
            message:
              'La imagen seleccionada como evidencia debe ser una carga confirmada del usuario autenticado.'
          })
        ],
        meta: expect.objectContaining({ path: '/v1/reviews' })
      })
    );
  });

  it('exposes public court reviews without authentication in the standard envelope', async () => {
    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reviews`
    });

    expect(response.statusCode).toBe(200);
    expect(prismaService.court.findFirst).toHaveBeenCalledWith({
      where: {
        id: courtId,
        status: 'ACTIVE',
        deletedAt: null,
        isPublished: true,
        complex: {
          status: 'ACTIVE',
          deletedAt: null,
          isPublished: true
        }
      },
      select: { id: true }
    });
    expect(response.json()).toEqual({
      success: true,
      data: {
        summary: { totalReviews: 2, averageRating: 4.5 },
        items: [
          {
            reviewId: 'review-a',
            rating: 5,
            comment: 'Great court and lighting.',
            createdAt: '2026-07-02T18:00:00.000Z',
            reviewer: { displayName: 'Diego R.', initials: 'DR' }
          },
          {
            reviewId: 'review-b',
            rating: 4,
            comment: null,
            createdAt: '2026-07-01T18:00:00.000Z',
            reviewer: { displayName: 'Player', initials: 'PP' }
          }
        ]
      },
      errors: [],
      meta: expect.objectContaining({
        path: `/v1/courts/${courtId}/reviews`,
        pagination: { page: 1, pageSize: 10, totalItems: 2, totalPages: 1 }
      })
    });
  });

  it('returns an empty public reviews envelope when the court has no reviews', async () => {
    prismaService.review.count.mockResolvedValueOnce(0);
    prismaService.review.findMany.mockResolvedValueOnce([]);
    prismaService.$queryRaw.mockResolvedValueOnce([{ average: null }]);

    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reviews`
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      success: true,
      data: {
        summary: { totalReviews: 0, averageRating: null },
        items: []
      },
      errors: [],
      meta: expect.objectContaining({
        path: `/v1/courts/${courtId}/reviews`
      })
    });
  });

  it('returns 404 when the court is not publicly visible', async () => {
    prismaService.court.findFirst.mockResolvedValueOnce(null);

    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reviews`
    });

    expect(response.statusCode).toBe(404);
    expect(prismaService.review.findMany).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
            status: 404
          })
        ]
      })
    );
  });

  it('rejects a malformed court id before touching persistence', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/courts/not-a-uuid/reviews'
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findFirst).not.toHaveBeenCalled();
    expect(prismaService.review.findMany).not.toHaveBeenCalled();
  });

  it('maps a unique evidence-image persistence collision back to the review validation error', async () => {
    prismaService.review.create.mockRejectedValueOnce({
      code: 'P2002',
      meta: { target: ['evidenceImageUploadId'] }
    });

    const response = await app.inject({
      method: 'POST',
      url: '/v1/reviews',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        reservationId,
        rating: 1,
        comment: 'The field was unsafe.',
        evidenceImageUploadId
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
            status: 400,
            message:
              'La imagen seleccionada como evidencia debe ser una carga confirmada del usuario autenticado.'
          })
        ],
        meta: expect.objectContaining({ path: '/v1/reviews' })
      })
    );
  });
});

function createPrismaMock() {
  return {
    reservation: {
      findFirst: jest.fn().mockResolvedValue({
        id: reservationId,
        startsAt: new Date('2026-07-02T20:00:00.000Z'),
        endsAt: new Date('2026-07-02T21:00:00.000Z'),
        court: {
          name: 'Cancha A',
          imageUpload: {
            objectKey: 'uploads/court-image/player-sub/2026/07/court-a.png'
          },
          complex: {
            name: 'Moravia FC'
          }
        }
      }),
      findUnique: jest.fn().mockResolvedValue({
        id: reservationId,
        userId: 'user-id',
        status: 'COMPLETED',
        completedAt: new Date('2026-07-03T01:00:00.000Z'),
        review: null
      })
    },
    review: {
      findFirst: jest.fn().mockResolvedValue(null),
      count: jest.fn().mockResolvedValue(2),
      findMany: jest.fn().mockResolvedValue([
        {
          id: 'review-a',
          rating: 5,
          comment: 'Great court and lighting.',
          createdAt: new Date('2026-07-02T18:00:00.000Z'),
          reservation: { user: { name: 'Diego Rivera' } }
        },
        {
          id: 'review-b',
          rating: 4,
          comment: null,
          createdAt: new Date('2026-07-01T18:00:00.000Z'),
          reservation: { user: { name: null } }
        }
      ]),
      create: jest.fn().mockResolvedValue({
        id: 'review-id',
        reservationId,
        rating: 1,
        comment: 'The court lights failed during the entire reservation.',
        evidenceImageUploadId,
        createdAt: new Date('2026-07-03T02:00:00.000Z')
      })
    },
    court: {
      findFirst: jest.fn().mockResolvedValue({ id: courtId })
    },
    $queryRaw: jest.fn().mockResolvedValue([{ average: 4.5 }]),
    onModuleInit: jest.fn(),
    onModuleDestroy: jest.fn()
  };
}

function createImageUploadRepositoryMock(): jest.Mocked<IImageUploadRepository> {
  return {
    findById: jest.fn().mockResolvedValue(createEvidenceImageUpload()),
    saveConfirmedUpload: jest.fn(),
    listRecent: jest.fn().mockResolvedValue([])
  };
}

function createEvidenceImageUpload() {
  return ImageUploadEntity.fromPersistence({
    id: evidenceImageUploadId,
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
