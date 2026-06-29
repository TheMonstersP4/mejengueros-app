import { randomUUID } from 'node:crypto';

import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { FastifyAdapter } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';

import { configureValidation } from '@/bootstrap/validation';
import type { ITokenVerifierPort } from '@/modules/auth/application/ports/token-verifier.port';
import { TOKEN_VERIFIER_PORT } from '@/modules/auth/application/ports/token-verifier.port';
import { CLOCK, type IClock } from '@/shared/application/clock/clock.port';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

const DEFAULT_TEST_DATABASE_URL = 'postgresql://user:password@localhost:5432/appdb';
const liveDatabaseUrl = process.env.DATABASE_URL;
const runLiveDatabaseHttpIntegration =
  typeof liveDatabaseUrl === 'string' &&
  liveDatabaseUrl.length > 0 &&
  liveDatabaseUrl !== DEFAULT_TEST_DATABASE_URL;

(runLiveDatabaseHttpIntegration ? describe : describe.skip)('reservations HTTP DB integration', () => {
  const originalEnv = {
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
  let prismaService: PrismaService;
  let tokenVerifier: jest.Mocked<ITokenVerifierPort>;
  let fixture: IReservationFixture;
  let currentNow: Date;

  beforeAll(async () => {
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
      verify: jest.fn()
    };
    const clock: IClock = {
      now: () => currentNow
    };

    const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(CLOCK)
      .useValue(clock)
      .overrideProvider(TOKEN_VERIFIER_PORT)
      .useValue(tokenVerifier)
      .compile();

    app = moduleRef.createNestApplication<NestFastifyApplication>(
      new FastifyAdapter({ logger: false })
    );
    app.setGlobalPrefix('v1');
    configureValidation(app);

    await app.init();
    await app.getHttpAdapter().getInstance().ready();

    prismaService = app.get(PrismaService);
  });

  beforeEach(async () => {
    jest.clearAllMocks();
    currentNow = new Date('2026-06-30T10:00:00.000Z');
    fixture = await seedReservationFixture(prismaService);
    tokenVerifier.verify.mockResolvedValue(createAuthenticatedUser('player-sub'));
  });

  afterEach(async () => {
    await cleanupReservationFixture(prismaService, fixture);
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

  it('creates one confirmed reservation and excludes it from the reservable slots query', async () => {
    const createResponse = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        courtId: fixture.courtId,
        startsAt: '2026-07-01T18:00:00.000Z'
      }
    });

    expect(createResponse.statusCode).toBe(201);

    const persistedReservation = await prismaService.reservation.findFirst({
      where: {
        courtId: fixture.courtId,
        startsAt: new Date('2026-07-01T18:00:00.000Z'),
        status: 'CONFIRMED'
      }
    });

    expect(persistedReservation).toMatchObject({
      courtId: fixture.courtId,
      userId: fixture.playerUserId,
      status: 'CONFIRMED'
    });

    const slotsResponse = await app.inject({
      method: 'GET',
      url: `/v1/courts/${fixture.courtId}/reservable-slots?date=2026-07-01`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(slotsResponse.statusCode).toBe(200);
    expect(slotsResponse.json().data.slots).toEqual([
      {
        startsAt: '2026-07-01T19:00:00.000Z',
        endsAt: '2026-07-01T20:00:00.000Z'
      }
    ]);
  });

  it('returns a conflict when the same confirmed slot is requested twice', async () => {
    await prismaService.reservation.create({
      data: {
        userId: fixture.playerUserId,
        courtId: fixture.courtId,
        startsAt: new Date('2026-07-01T18:00:00.000Z'),
        endsAt: new Date('2026-07-01T19:00:00.000Z'),
        status: 'CONFIRMED'
      }
    });

    const response = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        courtId: fixture.courtId,
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

  it('rejects stale or current same-day reservation creation before persistence', async () => {
    currentNow = new Date('2026-07-01T18:00:00.000Z');
    const reservationCreateSpy = jest.spyOn(prismaService.reservation, 'create');

    const response = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        courtId: fixture.courtId,
        startsAt: '2026-07-01T18:00:00.000Z'
      }
    });

    expect(response.statusCode).toBe(400);
    expect(reservationCreateSpy).not.toHaveBeenCalled();
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

  it('rejects offset reservation datetimes before they reach persistence', async () => {
    const reservationCreateSpy = jest.spyOn(prismaService.reservation, 'create');

    const response = await app.inject({
      method: 'POST',
      url: '/v1/reservations',
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        courtId: fixture.courtId,
        startsAt: '2026-07-01T12:00:00-06:00'
      }
    });

    expect(response.statusCode).toBe(400);
    expect(reservationCreateSpy).not.toHaveBeenCalled();
  });
});

interface IReservationFixture {
  playerUserId: string;
  courtId: string;
  complexId: string;
}

async function seedReservationFixture(prismaService: PrismaService): Promise<IReservationFixture> {
  const playerUserId = randomUUID();
  const ownerUserId = randomUUID();
  const complexId = randomUUID();
  const courtId = randomUUID();
  const suffix = randomUUID().slice(0, 8);

  await prismaService.user.create({
    data: {
      id: playerUserId,
      email: `reservation-player-${suffix}@example.test`,
      name: 'Reservation Player',
      identities: {
        create: {
          provider: 'Google',
          providerSubject: 'player-sub',
          emailAtLogin: `reservation-player-${suffix}@example.test`
        }
      }
    }
  });

  await prismaService.user.create({
    data: {
      id: ownerUserId,
      email: `reservation-owner-${suffix}@example.test`,
      name: 'Reservation Owner',
      identities: {
        create: {
          provider: 'Google',
          providerSubject: `owner-sub-${suffix}`,
          emailAtLogin: `reservation-owner-${suffix}@example.test`
        }
      }
    }
  });

  await prismaService.complex.create({
    data: {
      id: complexId,
      ownerId: ownerUserId,
      name: `Reservation Complex ${suffix}`,
      address: '123 Reservation Avenue'
    }
  });

  await prismaService.court.create({
    data: {
      id: courtId,
      complexId,
      name: `Court ${suffix}`,
      availability: {
        create: {
          startTime: new Date('1970-01-01T18:00:00.000Z'),
          endTime: new Date('1970-01-01T20:00:00.000Z'),
          days: {
            create: [{ day: 'WEDNESDAY' }]
          }
        }
      }
    }
  });

  return {
    playerUserId,
    courtId,
    complexId
  };
}

async function cleanupReservationFixture(
  prismaService: PrismaService,
  fixture: IReservationFixture | undefined
): Promise<void> {
  if (fixture == null) {
    return;
  }

  await prismaService.notification.deleteMany({
    where: {
      reservation: {
        courtId: fixture.courtId
      }
    }
  });

  await prismaService.review.deleteMany({
    where: {
      reservation: {
        courtId: fixture.courtId
      }
    }
  });

  await prismaService.reservation.deleteMany({
    where: {
      courtId: fixture.courtId
    }
  });

  await prismaService.court.deleteMany({
    where: {
      id: fixture.courtId
    }
  });

  await prismaService.complex.deleteMany({
    where: {
      id: fixture.complexId
    }
  });

  await prismaService.user.deleteMany({
    where: {
      id: {
        in: [fixture.playerUserId]
      }
    }
  });
}

function createAuthenticatedUser(sub: string) {
  return {
    sub,
    email: 'player@example.test',
    emailVerified: true,
    name: 'Reservation Player',
    pictureUrl: 'https://example.test/player.png',
    provider: 'Google',
    groups: ['players']
  };
}
