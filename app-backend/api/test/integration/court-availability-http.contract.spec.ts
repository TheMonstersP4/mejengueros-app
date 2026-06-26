import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { FastifyAdapter } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';
import type { ITokenVerifierPort } from '@/modules/auth/application/ports/token-verifier.port';
import { TOKEN_VERIFIER_PORT } from '@/modules/auth/application/ports/token-verifier.port';
import { configureValidation } from '@/bootstrap/validation';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

const ownedCourtId = '38fad3d5-0f6a-4c8a-a49a-c3dce07af6cf';

describe('court availability HTTP contract', () => {
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
        sub: 'owner-sub',
        email: 'owner@example.test',
        emailVerified: true,
        name: 'Owner User',
        pictureUrl: 'https://example.test/owner.png',
        provider: 'Google',
        groups: ['owners']
      })
    };

    const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(PrismaService)
      .useValue(prismaService)
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
  });

  beforeEach(() => {
    jest.clearAllMocks();
    prismaService = Object.assign(prismaService, createPrismaMock());
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

  it('returns owned court context even when availability is not configured yet', async () => {
    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/${ownedCourtId}/availability`,
      headers: { Authorization: 'Bearer valid-token' }
    });

    expect(response.statusCode).toBe(200);
    expect(prismaService.court.findFirst).toHaveBeenCalled();
    expect(response.json()).toEqual({
      success: true,
      data: {
        court: {
          id: ownedCourtId,
          name: 'Cancha 1',
          complexId: 'complex-id',
          complexName: 'Mejengas CR'
        },
        availability: null
      },
      errors: [],
      meta: expect.objectContaining({ path: `/v1/courts/${ownedCourtId}/availability` })
    });
  });

  it('saves one shared time range and selected weekday set for an owned court', async () => {
    const response = await app.inject({
      method: 'PUT',
      url: `/v1/courts/${ownedCourtId}/availability`,
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        days: ['MONDAY', 'FRIDAY'],
        startTime: '07:00',
        endTime: '10:00'
      }
    });

    expect(response.statusCode).toBe(200);
    expect(prismaService.courtAvailability.create).toHaveBeenCalledWith({
      data: {
        courtId: ownedCourtId,
        startTime: new Date('1970-01-01T07:00:00.000Z'),
        endTime: new Date('1970-01-01T10:00:00.000Z'),
        days: {
          create: [{ day: 'MONDAY' }, { day: 'FRIDAY' }]
        }
      }
    });
    expect(response.json()).toEqual({
      success: true,
      data: {
        court: {
          id: ownedCourtId,
          name: 'Cancha 1',
          complexId: 'complex-id',
          complexName: 'Mejengas CR'
        },
        availability: {
          days: ['MONDAY', 'FRIDAY'],
          startTime: '07:00',
          endTime: '10:00'
        }
      },
      errors: [],
      meta: expect.objectContaining({ path: `/v1/courts/${ownedCourtId}/availability` })
    });
  });

  it('rejects non whole-hour ranges before touching persistence', async () => {
    const response = await app.inject({
      method: 'PUT',
      url: `/v1/courts/${ownedCourtId}/availability`,
      headers: { Authorization: 'Bearer valid-token' },
      payload: {
        days: ['MONDAY'],
        startTime: '06:30',
        endTime: '09:00'
      }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findFirst).not.toHaveBeenCalled();
  });
});

function createPrismaMock() {
  return {
    court: {
      findFirst: jest.fn().mockResolvedValue({
        id: ownedCourtId,
        name: 'Cancha 1',
        complexId: 'complex-id',
        complex: { name: 'Mejengas CR' },
        availability: null
      })
    },
    courtAvailability: {
      create: jest.fn().mockResolvedValue({ id: 'availability-id' }),
      update: jest.fn().mockResolvedValue({ id: 'availability-id' })
    }
  };
}
