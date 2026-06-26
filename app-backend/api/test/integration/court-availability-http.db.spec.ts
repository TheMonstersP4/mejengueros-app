import { randomUUID } from 'node:crypto';

import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { FastifyAdapter } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';

import { configureValidation } from '@/bootstrap/validation';
import type { ITokenVerifierPort } from '@/modules/auth/application/ports/token-verifier.port';
import { TOKEN_VERIFIER_PORT } from '@/modules/auth/application/ports/token-verifier.port';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

const DEFAULT_TEST_DATABASE_URL = 'postgresql://user:password@localhost:5432/appdb';
const liveDatabaseUrl = process.env.DATABASE_URL;
const runLiveDatabaseHttpIntegration =
  typeof liveDatabaseUrl === 'string' &&
  liveDatabaseUrl.length > 0 &&
  liveDatabaseUrl !== DEFAULT_TEST_DATABASE_URL;

(runLiveDatabaseHttpIntegration ? describe : describe.skip)(
  'court availability HTTP DB integration',
  () => {
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
    let fixture: IAvailabilityFixture;

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

      const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
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
      fixture = await seedAvailabilityFixture(prismaService);
      tokenVerifier.verify.mockResolvedValue(createAuthenticatedUser('owner-sub'));
    });

    afterEach(async () => {
      await cleanupAvailabilityFixture(prismaService, fixture);
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

    it('persists one owned court availability through PUT and returns it through GET', async () => {
      const putResponse = await app.inject({
        method: 'PUT',
        url: `/v1/courts/${fixture.courtId}/availability`,
        headers: { Authorization: 'Bearer valid-token' },
        payload: {
          days: ['MONDAY', 'FRIDAY'],
          startTime: '07:00',
          endTime: '10:00'
        }
      });

      expect(putResponse.statusCode).toBe(200);
      expect(putResponse.json()).toEqual({
        success: true,
        data: {
          court: {
            id: fixture.courtId,
            name: fixture.courtName,
            complexId: fixture.complexId,
            complexName: fixture.complexName
          },
          availability: {
            days: ['MONDAY', 'FRIDAY'],
            startTime: '07:00',
            endTime: '10:00'
          }
        },
        errors: [],
        meta: expect.objectContaining({ path: `/v1/courts/${fixture.courtId}/availability` })
      });

      const persistedAvailability = await prismaService.courtAvailability.findUnique({
        where: { courtId: fixture.courtId },
        select: {
          courtId: true,
          startTime: true,
          endTime: true,
          days: {
            orderBy: { day: 'asc' },
            select: { day: true }
          },
          court: {
            select: {
              id: true,
              name: true,
              complexId: true,
              complex: {
                select: {
                  name: true
                }
              }
            }
          }
        }
      });

      expect(persistedAvailability).toMatchObject({
        courtId: fixture.courtId,
        court: {
          id: fixture.courtId,
          name: fixture.courtName,
          complexId: fixture.complexId,
          complex: {
            name: fixture.complexName
          }
        }
      });
      expect(persistedAvailability?.startTime.toISOString()).toBe('1970-01-01T07:00:00.000Z');
      expect(persistedAvailability?.endTime.toISOString()).toBe('1970-01-01T10:00:00.000Z');
      expect(persistedAvailability?.days).toHaveLength(2);
      expect(persistedAvailability?.days.map(({ day }) => day)).toEqual(
        expect.arrayContaining(['MONDAY', 'FRIDAY'])
      );

      const getResponse = await app.inject({
        method: 'GET',
        url: `/v1/courts/${fixture.courtId}/availability`,
        headers: { Authorization: 'Bearer valid-token' }
      });

      expect(getResponse.statusCode).toBe(200);
      expect(getResponse.json()).toEqual({
        success: true,
        data: {
          court: {
            id: fixture.courtId,
            name: fixture.courtName,
            complexId: fixture.complexId,
            complexName: fixture.complexName
          },
          availability: {
            days: expect.arrayContaining(['MONDAY', 'FRIDAY']),
            startTime: '07:00',
            endTime: '10:00'
          }
        },
        errors: [],
        meta: expect.objectContaining({ path: `/v1/courts/${fixture.courtId}/availability` })
      });
      expect(getResponse.json().data.availability.days).toHaveLength(2);
    });

    it('rejects saving availability for a court that is not owned by the authenticated user', async () => {
      tokenVerifier.verify.mockResolvedValue(createAuthenticatedUser('stranger-sub'));

      const response = await app.inject({
        method: 'PUT',
        url: `/v1/courts/${fixture.courtId}/availability`,
        headers: { Authorization: 'Bearer valid-token' },
        payload: {
          days: ['TUESDAY'],
          startTime: '08:00',
          endTime: '11:00'
        }
      });

      expect(response.statusCode).toBe(404);
      expect(response.json()).toEqual({
        success: false,
        data: null,
        errors: [
          {
            code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
            message: 'Court not found for the authenticated owner.',
            status: 404,
            type: 'urn:problem-type:backend:resource-not-found'
          }
        ],
        meta: expect.objectContaining({ path: `/v1/courts/${fixture.courtId}/availability` })
      });

      await expect(
        prismaService.courtAvailability.findUnique({
          where: { courtId: fixture.courtId }
        })
      ).resolves.toBeNull();
    });
  }
);

interface IAvailabilityFixture {
  ownerUserId: string;
  strangerUserId: string;
  complexId: string;
  complexName: string;
  courtId: string;
  courtName: string;
}

async function seedAvailabilityFixture(
  prismaService: PrismaService
): Promise<IAvailabilityFixture> {
  const ownerUserId = randomUUID();
  const strangerUserId = randomUUID();
  const complexId = randomUUID();
  const courtId = randomUUID();
  const suffix = randomUUID().slice(0, 8);

  await prismaService.user.create({
    data: {
      id: ownerUserId,
      email: `availability-owner-${suffix}@example.test`,
      name: 'Availability Owner',
      identities: {
        create: {
          provider: 'Google',
          providerSubject: 'owner-sub',
          emailAtLogin: `availability-owner-${suffix}@example.test`
        }
      }
    }
  });

  await prismaService.user.create({
    data: {
      id: strangerUserId,
      email: `availability-stranger-${suffix}@example.test`,
      name: 'Availability Stranger',
      identities: {
        create: {
          provider: 'Google',
          providerSubject: 'stranger-sub',
          emailAtLogin: `availability-stranger-${suffix}@example.test`
        }
      }
    }
  });

  await prismaService.complex.create({
    data: {
      id: complexId,
      ownerId: ownerUserId,
      name: `Availability Complex ${suffix}`,
      address: '123 Availability Avenue'
    }
  });

  await prismaService.court.create({
    data: {
      id: courtId,
      complexId,
      name: `Court ${suffix}`
    }
  });

  return {
    ownerUserId,
    strangerUserId,
    complexId,
    complexName: `Availability Complex ${suffix}`,
    courtId,
    courtName: `Court ${suffix}`
  };
}

async function cleanupAvailabilityFixture(
  prismaService: PrismaService,
  fixture: IAvailabilityFixture | undefined
): Promise<void> {
  if (fixture == null) {
    return;
  }

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
  await prismaService.userIdentity.deleteMany({
    where: {
      userId: {
        in: [fixture.ownerUserId, fixture.strangerUserId]
      }
    }
  });
  await prismaService.user.deleteMany({
    where: {
      id: {
        in: [fixture.ownerUserId, fixture.strangerUserId]
      }
    }
  });
}

function createAuthenticatedUser(sub: string) {
  return {
    sub,
    email: `${sub}@example.test`,
    emailVerified: true,
    name: `User ${sub}`,
    pictureUrl: `https://example.test/${sub}.png`,
    provider: 'Google',
    groups: ['owners']
  };
}
