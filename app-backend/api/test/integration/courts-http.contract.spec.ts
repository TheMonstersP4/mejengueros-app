import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { FastifyAdapter } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';
import { configureValidation } from '@/bootstrap/validation';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

describe('public court catalog HTTP contract', () => {
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

  beforeAll(async () => {
    process.env.DATABASE_URL =
      'postgresql://test:test@localhost:5432/test?schema=mejengueros';
    process.env.AWS_REGION = 'us-east-1';
    process.env.COGNITO_USER_POOL_ID = 'us-east-1_test';
    process.env.COGNITO_CLIENT_ID = 'test-client-id';
    process.env.APP_S3_BUCKET_NAME = 'test-bucket';
    process.env.APP_S3_KEY_PREFIX = 'test/uploads';
    process.env.APP_S3_PROFILE_IMAGE_MAX_BYTES = '5242880';
    process.env.APP_S3_ALLOWED_IMAGE_MIME_TYPES =
      'image/jpeg,image/png,image/webp';
    process.env.WEBSOCKET_CONNECTIONS_TABLE_NAME = 'test-websocket-connections';

    const { AppModule } = await import('@/app.module');

    prismaService = createPrismaMock();

    const moduleRef = await Test.createTestingModule({
      imports: [AppModule]
    })
      .overrideProvider(PrismaService)
      .useValue(prismaService)
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

  it('returns the public catalog without requiring authentication', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/courts/catalog?q=nogales&provinceId=3f91fe4d-a23b-4f85-ae1a-90db47d624f1&cantonId=1f6adf24-ea42-4c49-9179-c5f73fef7a41'
    });

    expect(response.statusCode).toBe(200);
    expect(prismaService.canton.findFirst).toHaveBeenCalledWith({
      where: {
        id: '1f6adf24-ea42-4c49-9179-c5f73fef7a41',
        provinceId: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1'
      },
      select: { id: true }
    });
    expect(prismaService.court.findMany).toHaveBeenCalledWith(
      expect.objectContaining({
        take: 50,
        where: expect.objectContaining({
          status: 'ACTIVE',
          isPublished: true
        })
      })
    );
    expect(prismaService.$queryRaw).toHaveBeenCalledTimes(1);
    expect(response.json()).toEqual({
      success: true,
      data: [
        {
          courtId: 'court-id',
          courtName: 'Cancha 1',
          complexId: 'complex-id',
          complexName: 'Complejo Los Nogales',
          province: {
            id: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1',
            name: 'San José'
          },
          canton: {
            id: '1f6adf24-ea42-4c49-9179-c5f73fef7a41',
            name: 'Escazú'
          },
          services: ['Sintetico', 'Iluminacion', 'Parqueo'],
          rating: {
            average: 4,
            count: 2
          },
          isReservableToday: true,
          imageUrl: null
        }
      ],
      errors: [],
      meta: expect.objectContaining({
        path: '/v1/courts/catalog?q=nogales&provinceId=3f91fe4d-a23b-4f85-ae1a-90db47d624f1&cantonId=1f6adf24-ea42-4c49-9179-c5f73fef7a41'
      })
    });
  });

  it('rejects a canton filter that does not belong to the selected province', async () => {
    prismaService.canton.findFirst.mockResolvedValue(null);

    const response = await app.inject({
      method: 'GET',
      url: '/v1/courts/catalog?provinceId=3f91fe4d-a23b-4f85-ae1a-90db47d624f1&cantonId=1f6adf24-ea42-4c49-9179-c5f73fef7a41'
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findMany).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            status: 400,
            message:
              'Selected canton "1f6adf24-ea42-4c49-9179-c5f73fef7a41" does not belong to province "3f91fe4d-a23b-4f85-ae1a-90db47d624f1".'
          })
        ]
      })
    );
  });

  it('returns a successful empty response when no catalog courts match', async () => {
    prismaService.court.findMany.mockResolvedValue([]);

    const response = await app.inject({
      method: 'GET',
      url: '/v1/courts/catalog?q=no-results'
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      success: true,
      data: [],
      errors: [],
      meta: expect.objectContaining({
        path: '/v1/courts/catalog?q=no-results'
      })
    });
  });

  function createPrismaMock() {
    return {
      $queryRaw: jest.fn().mockResolvedValue([{ courtId: 'court-id', average: 4, count: 2 }]),
      canton: {
        findFirst: jest.fn().mockResolvedValue({ id: '1f6adf24-ea42-4c49-9179-c5f73fef7a41' })
      },
      court: {
        findMany: jest.fn().mockResolvedValue([
          {
            id: 'court-id',
            name: 'Cancha 1',
            services: [
              { serviceCatalog: { name: 'Iluminacion' } },
              { serviceCatalog: { name: 'Sintetico' } }
            ],
            complex: {
              id: 'complex-id',
              name: 'Complejo Los Nogales',
              province: {
                id: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1',
                name: 'San José'
              },
              canton: {
                id: '1f6adf24-ea42-4c49-9179-c5f73fef7a41',
                name: 'Escazú'
              },
              services: [{ serviceCatalog: { name: 'Parqueo' } }]
            },
            availability: {
              days: [
                {
                  day: [
                    'SUNDAY',
                    'MONDAY',
                    'TUESDAY',
                    'WEDNESDAY',
                    'THURSDAY',
                    'FRIDAY',
                    'SATURDAY'
                  ][new Date().getDay()]
                }
              ]
            }
          }
        ])
      },
      onModuleInit: jest.fn(),
      onModuleDestroy: jest.fn()
    };
  }
});
