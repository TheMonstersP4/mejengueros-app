import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { FastifyAdapter } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';
import type { ITokenVerifierPort } from '@/modules/auth/application/ports/token-verifier.port';
import { TOKEN_VERIFIER_PORT } from '@/modules/auth/application/ports/token-verifier.port';
import { SyncAuthenticatedUserUseCase } from '@/modules/users/application/use-cases/sync-authenticated-user.use-case';
import { configureValidation } from '@/bootstrap/validation';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { CLOCK, type IClock } from '@/shared/application/clock/clock.port';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

const courtId = '38fad3d5-0f6a-4c8a-a49a-c3dce07af6cf';

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
        email: 'player@example.test'
      })
    } as unknown as jest.Mocked<SyncAuthenticatedUserUseCase>;
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
      email: 'player@example.test'
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
        startsAt: '2026-07-01T18:00:00.000Z'
      }
    });

    expect(response.statusCode).toBe(201);
    expect(prismaService.reservation.create).toHaveBeenCalledWith({
      data: {
        userId: 'user-id',
        courtId,
        startsAt: new Date('2026-07-01T18:00:00.000Z'),
        endsAt: new Date('2026-07-01T19:00:00.000Z'),
        status: 'CONFIRMED'
      }
    });
    expect(response.json()).toEqual({
      success: true,
      data: {
        id: 'reservation-id',
        courtId,
        startsAt: '2026-07-01T18:00:00.000Z',
        endsAt: '2026-07-01T19:00:00.000Z',
        status: 'CONFIRMED'
      },
      errors: [],
      meta: expect.objectContaining({ path: '/v1/reservations' })
    });
  });

  it('returns a safe conflict when the slot is already confirmed', async () => {
    prismaService.reservation.create.mockRejectedValue({ code: 'P2002' });

    const response = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        courtId,
        startsAt: '2026-07-01T18:00:00.000Z'
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

  it('rejects stale or current same-day reservation creation before touching persistence', async () => {
    currentNow = new Date('2026-07-01T18:00:00.000Z');

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
          message: 'Reservation start time must be strictly in the future.',
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
            startsAt: '2026-07-01T18:00:00.000Z',
            endsAt: '2026-07-01T19:00:00.000Z'
          },
          {
            startsAt: '2026-07-01T20:00:00.000Z',
            endsAt: '2026-07-01T21:00:00.000Z'
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
});

function createPrismaMock() {
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
      })
    },
    reservation: {
      create: jest.fn().mockResolvedValue({
        id: 'reservation-id',
        userId: 'user-id',
        courtId,
        startsAt: new Date('2026-07-01T18:00:00.000Z'),
        endsAt: new Date('2026-07-01T19:00:00.000Z'),
        status: 'CONFIRMED'
      })
    }
  };
}
