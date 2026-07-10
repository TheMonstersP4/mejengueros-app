import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { FastifyAdapter } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';
import { configureValidation } from '@/bootstrap/validation';
import { COURT_CATALOG_TODAY_PROVIDER } from '@/modules/courts/infrastructure/persistence/prisma-court-catalog.repository';
import {
  FILE_READ_URL_PORT,
  type IFileReadUrlPort
} from '@/modules/files/application/ports/file-read-url.port';
import { StorageInspectionError } from '@/modules/files/infrastructure/errors/storage-inspection.error';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

describe('public court catalog HTTP contract', () => {
  const fixedMonday = new Date('2026-06-22T12:00:00.000Z');
  let currentNow = fixedMonday;
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
  let fileReadUrl: jest.Mocked<IFileReadUrlPort>;
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
    fileReadUrl = {
      createReadUrl: jest.fn().mockResolvedValue(
        'https://read.example.test/courts/court-id.jpg'
      )
    };

    const moduleRef = await Test.createTestingModule({
      imports: [AppModule]
    })
      .overrideProvider(COURT_CATALOG_TODAY_PROVIDER)
      .useValue(() => currentNow)
      .overrideProvider(FILE_READ_URL_PORT)
      .useValue(fileReadUrl)
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
    currentNow = fixedMonday;
    prismaService = Object.assign(prismaService, createPrismaMock());
    fileReadUrl.createReadUrl.mockResolvedValue(
      'https://read.example.test/courts/court-id.jpg'
    );
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
        skip: 0,
        take: 10,
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
          imageUrl: 'https://read.example.test/courts/court-id.jpg'
        }
      ],
      errors: [],
      meta: expect.objectContaining({
        path: '/v1/courts/catalog?q=nogales&provinceId=3f91fe4d-a23b-4f85-ae1a-90db47d624f1&cantonId=1f6adf24-ea42-4c49-9179-c5f73fef7a41',
        pagination: {
          page: 1,
          pageSize: 10,
          totalItems: 1,
          totalPages: 1
        }
      })
    });
    expect(fileReadUrl.createReadUrl).toHaveBeenCalledWith(
      'test/uploads/court-image/court-id.jpg'
    );
  });

  it('applies the requested pagination window and reports next-page metadata', async () => {
    prismaService.court.count.mockResolvedValue(45);

    const response = await app.inject({
      method: 'GET',
      url: '/v1/courts/catalog?page=2&pageSize=20'
    });

    expect(response.statusCode).toBe(200);
    expect(prismaService.court.findMany).toHaveBeenCalledWith(
      expect.objectContaining({
        skip: 20,
        take: 20,
        orderBy: [{ complex: { name: 'asc' } }, { name: 'asc' }, { id: 'asc' }]
      })
    );
    expect(response.json().meta.pagination).toEqual({
      page: 2,
      pageSize: 20,
      totalItems: 45,
      totalPages: 3
    });
  });

  it('rejects a pageSize above the allowed maximum', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/courts/catalog?pageSize=51'
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.court.findMany).not.toHaveBeenCalled();
  });

  it('returns imageUrl null and does not request a read URL when a catalog court has no image upload', async () => {
    prismaService.court.findMany.mockResolvedValue([
      {
        ...createCatalogCourtRow(),
        imageUpload: null
      }
    ]);

    const response = await app.inject({
      method: 'GET',
      url: '/v1/courts/catalog?q=no-image'
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual({
      success: true,
      data: [
        expect.objectContaining({
          courtId: 'court-id',
          imageUrl: null
        })
      ],
      errors: [],
      meta: expect.objectContaining({
        path: '/v1/courts/catalog?q=no-image'
      })
    });
    expect(fileReadUrl.createReadUrl).not.toHaveBeenCalled();
  });

  it('keeps Costa Rica same-day reservability at the UTC rollover boundary when a later slot still clears the minimum advance threshold', async () => {
    currentNow = new Date('2026-07-05T00:45:00.000Z');
    prismaService.court.findMany.mockResolvedValue([
      createCatalogCourtRow({
        availabilityDays: ['SATURDAY'],
        availabilityStartTime: '1970-01-01T19:00:00.000Z',
        availabilityEndTime: '1970-01-01T21:00:00.000Z'
      })
    ]);

    const response = await app.inject({
      method: 'GET',
      url: '/v1/courts/catalog?q=utc-rollover-boundary'
    });

    expect(response.statusCode).toBe(200);
    expect(prismaService.court.findMany).toHaveBeenCalledWith(
      expect.objectContaining({
        select: expect.objectContaining({
          reservations: {
            where: {
              status: 'CONFIRMED',
              startsAt: {
                gte: new Date('2026-07-04T06:00:00.000Z'),
                lt: new Date('2026-07-05T06:00:00.000Z')
              }
            },
            select: {
              startsAt: true
            }
          }
        })
      })
    );
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: true,
        data: [expect.objectContaining({ isReservableToday: true })]
      })
    );
  });

  it('returns isReservableToday false at the UTC rollover boundary when the only threshold-clearing same-day slot is already booked', async () => {
    currentNow = new Date('2026-07-05T00:45:00.000Z');
    prismaService.court.findMany.mockResolvedValue([
      createCatalogCourtRow({
        availabilityDays: ['SATURDAY'],
        availabilityStartTime: '1970-01-01T19:00:00.000Z',
        availabilityEndTime: '1970-01-01T21:00:00.000Z',
        reservations: [{ startsAt: new Date('2026-07-05T02:00:00.000Z') }]
      })
    ]);

    const response = await app.inject({
      method: 'GET',
      url: '/v1/courts/catalog?q=utc-rollover-booked-boundary'
    });

    expect(response.statusCode).toBe(200);
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: true,
        data: [expect.objectContaining({ isReservableToday: false })]
      })
    );
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

  it('fails fast with the standard 502 envelope when catalog image signing fails', async () => {
    fileReadUrl.createReadUrl.mockRejectedValue(
      new StorageInspectionError('test/uploads/court-image/court-id.jpg', new Error('signer failed'))
    );

    const response = await app.inject({
      method: 'GET',
      url: '/v1/courts/catalog?q=signer-failure'
    });

    expect(response.statusCode).toBe(502);
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.EXTERNAL_SERVICE_ERROR,
            status: 502,
            message: 'Unable to inspect the uploaded file right now.'
          })
        ],
        meta: expect.objectContaining({
          path: '/v1/courts/catalog?q=signer-failure'
        })
      })
    );
  });

  it('rejects an overlong q query with the global 400 envelope', async () => {
    const overlongQuery = 'n'.repeat(101);

    const response = await app.inject({
      method: 'GET',
      url: `/v1/courts/catalog?q=${overlongQuery}`
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.canton.findFirst).not.toHaveBeenCalled();
    expect(prismaService.court.findMany).not.toHaveBeenCalled();
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
          path: `/v1/courts/catalog?q=${overlongQuery}`
        })
      })
    );
  });

  it('rejects an invalid provinceId query with the global 400 envelope', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/courts/catalog?provinceId=not-a-uuid'
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.canton.findFirst).not.toHaveBeenCalled();
    expect(prismaService.court.findMany).not.toHaveBeenCalled();
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
          path: '/v1/courts/catalog?provinceId=not-a-uuid'
        })
      })
    );
  });

  it('rejects an invalid cantonId query with the global 400 envelope', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/courts/catalog?cantonId=not-a-uuid'
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.canton.findFirst).not.toHaveBeenCalled();
    expect(prismaService.court.findMany).not.toHaveBeenCalled();
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
          path: '/v1/courts/catalog?cantonId=not-a-uuid'
        })
      })
    );
  });

  function createPrismaMock() {
    return {
      $queryRaw: jest.fn().mockResolvedValue([{ courtId: 'court-id', average: 4, count: 2 }]),
      canton: {
        findFirst: jest.fn().mockResolvedValue({ id: '1f6adf24-ea42-4c49-9179-c5f73fef7a41' })
      },
      court: {
        count: jest.fn().mockResolvedValue(1),
        findMany: jest.fn().mockResolvedValue([createCatalogCourtRow()])
      },
      onModuleInit: jest.fn(),
      onModuleDestroy: jest.fn()
    };
  }

  function createCatalogCourtRow({
    availabilityDays = ['MONDAY'],
    availabilityStartTime = '1970-01-01T18:00:00.000Z',
    availabilityEndTime = '1970-01-01T21:00:00.000Z',
    reservations = [] as Array<{ startsAt: Date }>
  } = {}) {
    return {
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
        startTime: new Date(availabilityStartTime),
        endTime: new Date(availabilityEndTime),
        days: availabilityDays.map((day) => ({ day }))
      },
      reservations,
      imageUpload: {
        objectKey: 'test/uploads/court-image/court-id.jpg'
      }
    };
  }
});
