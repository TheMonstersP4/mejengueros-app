import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { FastifyAdapter } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';
import type { ITokenVerifierPort } from '@/modules/auth/application/ports/token-verifier.port';
import { TOKEN_VERIFIER_PORT } from '@/modules/auth/application/ports/token-verifier.port';
import type { IFileReadUrlPort } from '@/modules/files/application/ports/file-read-url.port';
import { FILE_READ_URL_PORT } from '@/modules/files/application/ports/file-read-url.port';
import { SyncAuthenticatedUserUseCase } from '@/modules/users/application/use-cases/sync-authenticated-user.use-case';
import { configureValidation } from '@/bootstrap/validation';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { CLOCK, type IClock } from '@/shared/application/clock/clock.port';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

const courtId = '38fad3d5-0f6a-4c8a-a49a-c3dce07af6cf';
const signedCourtImageUrls: Record<string, string> = {
  'uploads/court-image/player-sub/2026/07/court-a.png':
    'https://read.example.test/court-a.png',
  'uploads/court-image/player-sub/2026/07/court-b.png':
    'https://read.example.test/court-b.png',
  'uploads/court-image/player-sub/2026/07/court-c.png':
    'https://read.example.test/court-c.png'
};

describe('reservations HTTP contract', () => {
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
  let currentNow: Date;

  beforeAll(async () => {
    process.env.DATABASE_URL = 'postgresql://test:test@localhost:5432/test?schema=mejengueros';
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
      createReadUrl: jest.fn().mockImplementation(async (objectKey: string) => {
        return signedCourtImageUrls[objectKey] ?? `https://read.example.test/${objectKey}`;
      })
    } as jest.Mocked<IFileReadUrlPort>;
    const clock: IClock = {
      now: () => currentNow
    };

    const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(PrismaService)
      .useValue(prismaService)
      .overrideProvider(CLOCK)
      .useValue(clock)
      .overrideProvider(TOKEN_VERIFIER_PORT)
      .useValue(tokenVerifier)
      .overrideProvider(SyncAuthenticatedUserUseCase)
      .useValue(syncAuthenticatedUser)
      .overrideProvider(FILE_READ_URL_PORT)
      .useValue(fileReadUrl)
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
    currentNow = new Date('2026-06-30T10:00:00.000Z');
    prismaService = Object.assign(prismaService, createPrismaMock());
    syncAuthenticatedUser.execute.mockResolvedValue({
      id: 'user-id',
      email: 'player@example.test',
      roles: []
    });
    fileReadUrl.createReadUrl.mockImplementation(async (objectKey: string) => {
      return signedCourtImageUrls[objectKey] ?? `https://read.example.test/${objectKey}`;
    });
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

  it('creates one confirmed reservation for a reservable slot', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        courtId,
        startsAt: '2026-07-02T00:00:00.000Z'
      }
    });

    expect(response.statusCode).toBe(201);
    expect(prismaService.reservation.create).toHaveBeenCalledWith({
      data: {
        userId: 'user-id',
        courtId,
        startsAt: new Date('2026-07-02T00:00:00.000Z'),
        endsAt: new Date('2026-07-02T01:00:00.000Z'),
        status: 'CONFIRMED'
      }
    });
    expect(response.json()).toEqual({
      success: true,
      data: {
        id: 'reservation-id',
        courtId,
        startsAt: '2026-07-02T00:00:00.000Z',
        endsAt: '2026-07-02T01:00:00.000Z',
        status: 'CONFIRMED'
      },
      errors: [],
      meta: expect.objectContaining({ path: '/v1/reservations' })
    });
  });

  it('returns grouped my reservations in the standard envelope', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/reservations/my',
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(syncAuthenticatedUser.execute).toHaveBeenCalledWith({
      sub: 'player-sub',
      email: 'player@example.test',
      emailVerified: true,
      name: 'Player One',
      pictureUrl: 'https://example.test/player.png',
      provider: 'Google',
      groups: ['players']
    });
    expect(response.json()).toEqual({
      success: true,
      data: {
        upcoming: [
          {
            id: 'upcoming-reservation-id',
            complexName: 'Moravia FC',
            courtName: 'Cancha A',
            imageUrl: 'https://read.example.test/court-a.png',
            startsAt: '2026-07-10T18:00:00.000Z',
            endsAt: '2026-07-10T19:00:00.000Z',
            status: 'CONFIRMED',
            section: 'UPCOMING',
            reviewStatus: 'NOT_APPLICABLE',
            canReview: false,
            hasReview: false
          }
        ],
        finalized: [
          {
            id: 'completed-reviewed-id',
            complexName: 'Moravia FC',
            courtName: 'Cancha C',
            imageUrl: 'https://read.example.test/court-c.png',
            startsAt: '2026-07-04T18:00:00.000Z',
            endsAt: '2026-07-04T19:00:00.000Z',
            status: 'COMPLETED',
            section: 'FINALIZED',
            reviewStatus: 'REVIEWED',
            canReview: false,
            hasReview: true,
            indicatorKey: 'already_reviewed',
            indicatorLabel: 'Ya dejaste tu reseña'
          },
          {
            id: 'completed-pending-review-id',
            complexName: 'Moravia FC',
            courtName: 'Cancha B',
            imageUrl: 'https://read.example.test/court-b.png',
            startsAt: '2026-07-03T18:00:00.000Z',
            endsAt: '2026-07-03T19:00:00.000Z',
            status: 'COMPLETED',
            section: 'FINALIZED',
            reviewStatus: 'PENDING_REVIEW',
            canReview: true,
            hasReview: false,
            primaryActionKey: 'leave_review',
            primaryActionLabel: 'Dejar reseña'
          }
        ]
      },
      errors: [],
      meta: expect.objectContaining({ path: '/v1/reservations/my' })
    });
    expect(response.json().data.upcoming).toEqual(
      expect.not.arrayContaining([
        expect.objectContaining({ id: 'cancelled-reservation-id' })
      ])
    );
    expect(response.json().data.finalized).toEqual(
      expect.not.arrayContaining([
        expect.objectContaining({ id: 'completed-missing-completed-at-id' })
      ])
    );
  });

  it('returns empty arrays when the authenticated user has no reservations', async () => {
    prismaService.reservation.findMany.mockResolvedValueOnce([]).mockResolvedValueOnce([]);

    const response = await app.inject({
      method: 'GET',
      url: '/v1/reservations/my',
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      success: true,
      data: {
        upcoming: [],
        finalized: []
      },
      errors: [],
      meta: expect.objectContaining({ path: '/v1/reservations/my' })
    });
  });

  it('returns a bounded my reservations screen snapshot capped to 20 upcoming and 20 finalized cards', async () => {
    const upcomingRecords = Array.from({ length: 24 }, (_, index) =>
      createReservationRecord({
        id: `upcoming-${String(index + 1).padStart(2, '0')}`,
        startsAt: new Date('2026-07-10T18:00:00.000Z'),
        endsAt: new Date('2026-07-10T19:00:00.000Z'),
        status: 'CONFIRMED',
        completedAt: null,
        review: null,
        courtName: `Cancha U${index + 1}`,
        imageObjectKey: `uploads/court-image/player-sub/2026/07/upcoming-${index + 1}.png`
      })
    );
    const finalizedRecords = Array.from({ length: 24 }, (_, index) =>
      createReservationRecord({
        id: `finalized-${String(index + 1).padStart(2, '0')}`,
        startsAt: new Date('2026-07-05T18:00:00.000Z'),
        endsAt: new Date('2026-07-05T19:00:00.000Z'),
        status: 'COMPLETED',
        completedAt: new Date('2026-07-05T19:05:00.000Z'),
        review: index % 2 === 0 ? null : { id: `review-${index + 1}` },
        courtName: `Cancha F${index + 1}`,
        imageObjectKey: `uploads/court-image/player-sub/2026/07/finalized-${index + 1}.png`
      })
    );

    prismaService.reservation.findMany.mockImplementation(
      async (query?: { where?: { status?: string }; take?: number; orderBy?: ReservationOrderBy }) => {
        const status = query?.where?.status;
        const source = status === 'CONFIRMED' ? upcomingRecords : finalizedRecords;

        return applyReservationQuery(source, query);
      }
    );

    const response = await app.inject({
      method: 'GET',
      url: '/v1/reservations/my',
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(response.json().data.upcoming).toHaveLength(20);
    expect(response.json().data.finalized).toHaveLength(20);
    expect(response.json().data.upcoming.map((reservation: { id: string }) => reservation.id)).toEqual(
      Array.from({ length: 20 }, (_, index) => `upcoming-${String(index + 1).padStart(2, '0')}`)
    );
    expect(response.json().data.finalized.map((reservation: { id: string }) => reservation.id)).toEqual(
      Array.from({ length: 20 }, (_, index) => `finalized-${String(index + 1).padStart(2, '0')}`)
    );
  });

  it('keeps my reservations cards when image URL signing fails by omitting imageUrl', async () => {
    fileReadUrl.createReadUrl.mockImplementation(async (objectKey: string) => {
      if (objectKey === 'uploads/court-image/player-sub/2026/07/court-c.png') {
        throw new Error('signed URL unavailable');
      }

      return signedCourtImageUrls[objectKey] ?? `https://read.example.test/${objectKey}`;
    });

    const response = await app.inject({
      method: 'GET',
      url: '/v1/reservations/my',
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toMatchObject({
      success: true,
      data: {
        upcoming: [
          {
            id: 'upcoming-reservation-id',
            imageUrl: 'https://read.example.test/court-a.png'
          }
        ],
        finalized: [
          {
            id: 'completed-reviewed-id'
          },
          {
            id: 'completed-pending-review-id',
            imageUrl: 'https://read.example.test/court-b.png'
          }
        ]
      }
    });
    expect(response.json().data.finalized[0]).not.toHaveProperty('imageUrl');
  });

  it('rejects unauthenticated my reservations access', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/reservations/my'
    });

    expect(response.statusCode).toBe(401);
  });

  it('returns owner reservations grouped and scoped to owned courts without review actions', async () => {
    prismaService.court.findMany = jest.fn().mockResolvedValue([{ id: courtId }]);

    const response = await app.inject({
      method: 'GET',
      url: '/v1/owners/me/reservations',
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      success: true,
      data: {
        selectedCourtId: null,
        upcoming: [
          {
            id: 'upcoming-reservation-id',
            complexName: 'Moravia FC',
            courtName: 'Cancha A',
            imageUrl: 'https://read.example.test/court-a.png',
            startsAt: '2026-07-10T18:00:00.000Z',
            endsAt: '2026-07-10T19:00:00.000Z',
            status: 'CONFIRMED',
            section: 'UPCOMING'
          }
        ],
        finalized: [
          {
            id: 'completed-reviewed-id',
            complexName: 'Moravia FC',
            courtName: 'Cancha C',
            imageUrl: 'https://read.example.test/court-c.png',
            startsAt: '2026-07-04T18:00:00.000Z',
            endsAt: '2026-07-04T19:00:00.000Z',
            status: 'COMPLETED',
            section: 'FINALIZED'
          },
          {
            id: 'completed-pending-review-id',
            complexName: 'Moravia FC',
            courtName: 'Cancha B',
            imageUrl: 'https://read.example.test/court-b.png',
            startsAt: '2026-07-03T18:00:00.000Z',
            endsAt: '2026-07-03T19:00:00.000Z',
            status: 'COMPLETED',
            section: 'FINALIZED'
          }
        ]
      },
      errors: [],
      meta: expect.objectContaining({ path: '/v1/owners/me/reservations' })
    });
  });

  it('applies the court filter and echoes the selected court id', async () => {
    prismaService.court.findMany = jest.fn().mockResolvedValue([{ id: courtId }]);

    const response = await app.inject({
      method: 'GET',
      url: `/v1/owners/me/reservations?courtId=${courtId}`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(response.json().data.selectedCourtId).toBe(courtId);
  });

  it('returns 404 when filtering by a court the owner does not own', async () => {
    prismaService.court.findMany = jest.fn().mockResolvedValue([]);
    const foreignCourtId = '11111111-1111-4111-8111-111111111111';

    const response = await app.inject({
      method: 'GET',
      url: `/v1/owners/me/reservations?courtId=${foreignCourtId}`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(404);
    expect(response.json().errors[0].code).toBe(APP_ERROR_CODES.RESOURCE_NOT_FOUND);
  });

  it('rejects a malformed court id before touching persistence', async () => {
    prismaService.court.findMany = jest.fn();

    const response = await app.inject({
      method: 'GET',
      url: '/v1/owners/me/reservations?courtId=not-a-uuid',
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findMany).not.toHaveBeenCalled();
  });

  it('rejects unauthenticated owner reservations access', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/owners/me/reservations'
    });

    expect(response.statusCode).toBe(401);
  });

  it('returns a safe conflict when the slot is already confirmed', async () => {
    prismaService.reservation.create.mockRejectedValue({ code: 'P2002' });

    const response = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        courtId,
        startsAt: '2026-07-02T00:00:00.000Z'
      }
    });

    expect(response.statusCode).toBe(409);
    expect(response.json()).toEqual({
      success: false,
      data: null,
      errors: [
        {
          code: APP_ERROR_CODES.CONFLICT,
          message: 'This court already has a confirmed reservation for the selected start time.',
          status: 409,
          type: 'urn:problem-type:backend:conflict'
        }
      ],
      meta: expect.objectContaining({ path: '/v1/reservations' })
    });
  });

  it('rejects reservation starts outside configured availability', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        courtId,
        startsAt: '2026-07-01T21:00:00.000Z'
      }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.reservation.create).not.toHaveBeenCalled();
  });

  it('rejects same-day reservation creation that does not clear the 30-minute threshold before touching persistence', async () => {
    currentNow = new Date('2026-07-01T17:30:00.000Z');

    const response = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        courtId,
        startsAt: '2026-07-01T18:00:00.000Z'
      }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findFirst).not.toHaveBeenCalled();
    expect(prismaService.reservation.create).not.toHaveBeenCalled();
    expect(response.json()).toEqual({
      success: false,
      data: null,
      errors: [
        {
          code: APP_ERROR_CODES.VALIDATION_FAILED,
          message: 'Same-day reservation start time must be more than 30 minutes in the future.',
          status: 400,
          type: 'urn:problem-type:backend:validation-failed'
        }
      ],
      meta: expect.objectContaining({ path: '/v1/reservations' })
    });
  });

  it('rejects unauthenticated reservation creation', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      payload: {
        courtId,
        startsAt: '2026-07-01T18:00:00.000Z'
      }
    });

    expect(response.statusCode).toBe(401);
  });

  it('rejects invalid reservation requests before touching persistence', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        courtId: 'bad-id',
        startsAt: 'nope'
      }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findFirst).not.toHaveBeenCalled();
  });

  it('rejects offset reservation datetimes before touching persistence', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        courtId,
        startsAt: '2026-07-01T12:00:00-06:00'
      }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findFirst).not.toHaveBeenCalled();
  });

  it('rejects reservation date-only values before touching persistence', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        courtId,
        startsAt: '2026-07-01'
      }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findFirst).not.toHaveBeenCalled();
  });

  it('rejects reservation datetimes without an explicit UTC timezone before touching persistence', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        courtId,
        startsAt: '2026-07-01T18:00:00'
      }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findFirst).not.toHaveBeenCalled();
  });

  it('returns not found when creating a reservation for a missing court', async () => {
    prismaService.court.findFirst.mockResolvedValueOnce(null);

    const response = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        courtId,
        startsAt: '2026-07-01T18:00:00.000Z'
      }
    });

    expect(response.statusCode).toBe(404);
    expect(prismaService.reservation.create).not.toHaveBeenCalled();
    expect(response.json()).toEqual({
      success: false,
      data: null,
      errors: [
        {
          code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
          message: 'Court not found.',
          status: 404,
          type: 'urn:problem-type:backend:resource-not-found'
        }
      ],
      meta: expect.objectContaining({ path: '/v1/reservations' })
    });
  });

  it('returns only unbooked reservable slots for the selected court date', async () => {
    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reservable-slots?date=2026-07-01`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      success: true,
      data: {
        court: {
          id: courtId,
          name: 'Cancha 1',
          status: 'ACTIVE'
        },
        date: '2026-07-01',
        availabilityStatus: 'AVAILABLE',
        slots: [
          {
            startsAt: '2026-07-02T00:00:00.000Z',
            endsAt: '2026-07-02T01:00:00.000Z'
          },
          {
            startsAt: '2026-07-02T01:00:00.000Z',
            endsAt: '2026-07-02T02:00:00.000Z'
          },
          {
            startsAt: '2026-07-02T02:00:00.000Z',
            endsAt: '2026-07-02T03:00:00.000Z'
          }
        ]
      },
      errors: [],
      meta: expect.objectContaining({
        path: `/v1/courts/${courtId}/reservable-slots?date=2026-07-01`
      })
    });
  });

  it('excludes same-day slots at or inside the 30-minute threshold from the public reservable slots response while keeping later slots', async () => {
    currentNow = new Date('2026-07-02T00:30:00.000Z');
    prismaService.court.findFirst.mockResolvedValueOnce({
      id: courtId,
      name: 'Cancha 1',
      status: 'ACTIVE',
      complex: { status: 'ACTIVE' },
      availability: {
        startTime: new Date('1970-01-01T18:00:00.000Z'),
        endTime: new Date('1970-01-01T21:00:00.000Z'),
        days: [{ day: 'WEDNESDAY' }]
      },
      reservations: []
    });

    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reservable-slots?date=2026-07-01`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      success: true,
      data: {
        court: {
          id: courtId,
          name: 'Cancha 1',
          status: 'ACTIVE'
        },
        date: '2026-07-01',
        availabilityStatus: 'AVAILABLE',
        slots: [
          {
            startsAt: '2026-07-02T02:00:00.000Z',
            endsAt: '2026-07-02T03:00:00.000Z'
          }
        ]
      },
      errors: [],
      meta: expect.objectContaining({
        path: `/v1/courts/${courtId}/reservable-slots?date=2026-07-01`
      })
    });
  });

  it('rejects invalid reservable slot dates', async () => {
    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reservable-slots?date=2026-02-31`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findFirst).not.toHaveBeenCalled();
  });

  it('rejects unauthenticated reservable slot queries', async () => {
    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reservable-slots?date=2026-07-01`
    });

    expect(response.statusCode).toBe(401);
  });

  it('returns not found when the reservable slots court does not exist', async () => {
    prismaService.court.findFirst.mockResolvedValueOnce(null);

    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reservable-slots?date=2026-07-01`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(404);
    expect(response.json()).toEqual({
      success: false,
      data: null,
      errors: [
        {
          code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
          message: 'Court not found.',
          status: 404,
          type: 'urn:problem-type:backend:resource-not-found'
        }
      ],
      meta: expect.objectContaining({
        path: `/v1/courts/${courtId}/reservable-slots?date=2026-07-01`
      })
    });
  });

  it('returns reservable day discovery in the standard envelope', async () => {
    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reservable-days?from=2026-07-01&days=3`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      success: true,
      data: {
        court: {
          id: courtId,
          name: 'Cancha 1',
          status: 'ACTIVE'
        },
        from: '2026-07-01',
        days: 3,
        reservableDays: [
          {
            date: '2026-07-01',
            availabilityStatus: 'AVAILABLE',
            availableSlotsCount: 3
          }
        ]
      },
      errors: [],
      meta: expect.objectContaining({
        path: `/v1/courts/${courtId}/reservable-days?from=2026-07-01&days=3`
      })
    });
  });

  it('defaults omitted reservable day range to the safe bounded window', async () => {
    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reservable-days?from=2026-07-01`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      success: true,
      data: {
        court: {
          id: courtId,
          name: 'Cancha 1',
          status: 'ACTIVE'
        },
        from: '2026-07-01',
        days: 14,
        reservableDays: [
          {
            date: '2026-07-01',
            availabilityStatus: 'AVAILABLE',
            availableSlotsCount: 3
          },
          {
            date: '2026-07-08',
            availabilityStatus: 'AVAILABLE',
            availableSlotsCount: 3
          }
        ]
      },
      errors: [],
      meta: expect.objectContaining({
        path: `/v1/courts/${courtId}/reservable-days?from=2026-07-01`
      })
    });
  });

  it('rejects reservable day queries without the required from date', async () => {
    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reservable-days?days=14`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findFirst).not.toHaveBeenCalled();
  });

  it('rejects invalid reservable day start dates', async () => {
    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reservable-days?from=2026-02-31&days=14`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findFirst).not.toHaveBeenCalled();
  });

  it('rejects invalid reservable day ranges', async () => {
    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reservable-days?from=2026-07-01&days=0`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findFirst).not.toHaveBeenCalled();
  });

  it('rejects reservable day ranges above the bounded maximum', async () => {
    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reservable-days?from=2026-07-01&days=32`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findFirst).not.toHaveBeenCalled();
  });

  it('rejects unauthenticated reservable day queries', async () => {
    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reservable-days?from=2026-07-01&days=3`
    });

    expect(response.statusCode).toBe(401);
  });

  it('returns not found when the reservable days court does not exist', async () => {
    prismaService.court.findFirst.mockResolvedValueOnce(null);

    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reservable-days?from=2026-07-01&days=3`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(404);
    expect(response.json()).toEqual({
      success: false,
      data: null,
      errors: [
        {
          code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
          message: 'Court not found.',
          status: 404,
          type: 'urn:problem-type:backend:resource-not-found'
        }
      ],
      meta: expect.objectContaining({
        path: `/v1/courts/${courtId}/reservable-days?from=2026-07-01&days=3`
      })
    });
  });

  it('omits today from reservable day counts in the real HTTP response when every same-day slot is inside the threshold', async () => {
    currentNow = new Date('2026-07-02T02:30:00.000Z');
    prismaService.court.findFirst.mockResolvedValue({
      id: courtId,
      name: 'Cancha 1',
      status: 'ACTIVE',
      complex: { status: 'ACTIVE' },
      availability: {
        startTime: new Date('1970-01-01T18:00:00.000Z'),
        endTime: new Date('1970-01-01T21:00:00.000Z'),
        days: [{ day: 'WEDNESDAY' }, { day: 'THURSDAY' }]
      },
      reservations: []
    });

    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${courtId}/reservable-days?from=2026-07-01&days=2`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      success: true,
      data: {
        court: {
          id: courtId,
          name: 'Cancha 1',
          status: 'ACTIVE'
        },
        from: '2026-07-01',
        days: 2,
        reservableDays: [
          {
            date: '2026-07-02',
            availabilityStatus: 'AVAILABLE',
            availableSlotsCount: 3
          }
        ]
      },
      errors: [],
      meta: expect.objectContaining({
        path: `/v1/courts/${courtId}/reservable-days?from=2026-07-01&days=2`
      })
    });
  });
});

function createPrismaMock() {
  const reservationRecords = [
    createReservationRecord({
      id: 'upcoming-reservation-id',
      startsAt: new Date('2026-07-10T18:00:00.000Z'),
      endsAt: new Date('2026-07-10T19:00:00.000Z'),
      status: 'CONFIRMED',
      completedAt: null,
      review: null,
      courtName: 'Cancha A',
      imageObjectKey: 'uploads/court-image/player-sub/2026/07/court-a.png'
    }),
    createReservationRecord({
      id: 'completed-pending-review-id',
      startsAt: new Date('2026-07-03T18:00:00.000Z'),
      endsAt: new Date('2026-07-03T19:00:00.000Z'),
      status: 'COMPLETED',
      completedAt: new Date('2026-07-03T19:05:00.000Z'),
      review: null,
      courtName: 'Cancha B',
      imageObjectKey: 'uploads/court-image/player-sub/2026/07/court-b.png'
    }),
    createReservationRecord({
      id: 'completed-reviewed-id',
      startsAt: new Date('2026-07-04T18:00:00.000Z'),
      endsAt: new Date('2026-07-04T19:00:00.000Z'),
      status: 'COMPLETED',
      completedAt: new Date('2026-07-04T19:05:00.000Z'),
      review: { id: 'review-id' },
      courtName: 'Cancha C',
      imageObjectKey: 'uploads/court-image/player-sub/2026/07/court-c.png'
    }),
    createReservationRecord({
      id: 'completed-missing-completed-at-id',
      startsAt: new Date('2026-07-02T18:00:00.000Z'),
      endsAt: new Date('2026-07-02T19:00:00.000Z'),
      status: 'COMPLETED',
      completedAt: null,
      review: null,
      courtName: 'Cancha E',
      imageObjectKey: null
    }),
    createReservationRecord({
      id: 'cancelled-reservation-id',
      startsAt: new Date('2026-07-05T18:00:00.000Z'),
      endsAt: new Date('2026-07-05T19:00:00.000Z'),
      status: 'CANCELLED',
      completedAt: null,
      review: null,
      courtName: 'Cancha D',
      imageObjectKey: null
    })
  ];

  return {
    court: {
      findFirst: jest.fn().mockResolvedValue({
        id: courtId,
        name: 'Cancha 1',
        status: 'ACTIVE',
        complex: { status: 'ACTIVE' },
        availability: {
          startTime: new Date('1970-01-01T18:00:00.000Z'),
          endTime: new Date('1970-01-01T21:00:00.000Z'),
          days: [{ day: 'WEDNESDAY' }]
        },
        reservations: [{ startsAt: new Date('2026-07-01T19:00:00.000Z') }]
      }),
      findMany: jest.fn().mockResolvedValue([])
    },
    reservation: {
      findMany: jest.fn().mockImplementation(
        async (query?: {
          where?: { status?: string; completedAt?: { not: null } };
          take?: number;
          orderBy?: ReservationOrderBy;
        }) => {
          const status = query?.where?.status;
          const requiresCompletedAt = query?.where?.completedAt != null;
          const filtered = reservationRecords.filter((reservation) => {
            if (status != null && reservation.status !== status) {
              return false;
            }

            if (requiresCompletedAt && reservation.completedAt == null) {
              return false;
            }

            return true;
          });

          return applyReservationQuery(filtered, query);
        }
      ),
      create: jest.fn().mockResolvedValue({
        id: 'reservation-id',
        userId: 'user-id',
        courtId,
        startsAt: new Date('2026-07-02T00:00:00.000Z'),
        endsAt: new Date('2026-07-02T01:00:00.000Z'),
        status: 'CONFIRMED'
      })
    }
  };
}

type ReservationRecord = {
  id: string;
  startsAt: Date;
  endsAt: Date;
  status: string;
  completedAt: Date | null;
  review: { id: string } | null;
  court: {
    name: string;
    imageUpload: {
      objectKey: string;
    } | null;
    complex: {
      name: string;
    };
  };
};

type ReservationOrderBy =
  | { startsAt?: 'asc' | 'desc'; completedAt?: 'asc' | 'desc'; id?: 'asc' | 'desc' }
  | Array<{ startsAt?: 'asc' | 'desc'; completedAt?: 'asc' | 'desc'; id?: 'asc' | 'desc' }>;

function createReservationRecord(input: {
  id: string;
  startsAt: Date;
  endsAt: Date;
  status: string;
  completedAt: Date | null;
  review: { id: string } | null;
  courtName: string;
  imageObjectKey: string | null;
}): ReservationRecord {
  return {
    id: input.id,
    startsAt: input.startsAt,
    endsAt: input.endsAt,
    status: input.status,
    completedAt: input.completedAt,
    review: input.review,
    court: {
      name: input.courtName,
      imageUpload:
        input.imageObjectKey == null
          ? null
          : {
              objectKey: input.imageObjectKey
            },
      complex: {
        name: 'Moravia FC'
      }
    }
  };
}

function applyReservationQuery(
  reservations: ReservationRecord[],
  query?: { take?: number; orderBy?: ReservationOrderBy }
): ReservationRecord[] {
  const sorted = [...reservations];
  const orderBy = Array.isArray(query?.orderBy)
    ? query.orderBy
    : query?.orderBy == null
      ? []
      : [query.orderBy];

  sorted.sort((left, right) => compareReservationRecords(left, right, orderBy));

  return sorted.slice(0, query?.take ?? sorted.length);
}

function compareReservationRecords(
  left: ReservationRecord,
  right: ReservationRecord,
  orderBy: Array<{ startsAt?: 'asc' | 'desc'; completedAt?: 'asc' | 'desc'; id?: 'asc' | 'desc' }>
): number {
  for (const order of orderBy) {
    const [field, direction] = Object.entries(order)[0] ?? [];

    if (field == null || direction == null) {
      continue;
    }

    const comparison = compareOrderField(left, right, field as 'startsAt' | 'completedAt' | 'id');

    if (comparison !== 0) {
      return direction === 'asc' ? comparison : -comparison;
    }
  }

  return 0;
}

function compareOrderField(
  left: ReservationRecord,
  right: ReservationRecord,
  field: 'startsAt' | 'completedAt' | 'id'
): number {
  if (field === 'id') {
    return left.id.localeCompare(right.id);
  }

  const leftValue = left[field]?.getTime() ?? Number.NEGATIVE_INFINITY;
  const rightValue = right[field]?.getTime() ?? Number.NEGATIVE_INFINITY;

  return leftValue - rightValue;
}
