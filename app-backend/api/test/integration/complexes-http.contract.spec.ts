import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { FastifyAdapter } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';
import type { ITokenVerifierPort } from '@/modules/auth/application/ports/token-verifier.port';
import { TOKEN_VERIFIER_PORT } from '@/modules/auth/application/ports/token-verifier.port';
import { configureValidation } from '@/bootstrap/validation';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

describe('complex wizard HTTP contract', () => {
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

  const requestPayload = {
    complex: {
      name: 'North Sports Center',
      provinceId: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1',
      cantonId: '1f6adf24-ea42-4c49-9179-c5f73fef7a41',
      address: '123 Main Street',
      latitude: 9.935,
      longitude: -84.091,
      serviceIds: ['d76a5f20-83f0-4538-a1c8-4f7b60d0f4be']
    },
    firstCourt: {
      name: 'Court A',
      serviceIds: [
        'aab8a9f0-faf2-4e73-a8cb-6853f48cc9a7',
        'f96c0626-e055-4187-a100-c7d465f51f3b'
      ]
    }
  };

  let app: NestFastifyApplication;
  let prismaService: ReturnType<typeof createPrismaMock>;
  let tokenVerifier: jest.Mocked<ITokenVerifierPort>;

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
    tokenVerifier = {
      verify: jest.fn().mockResolvedValue({
        sub: 'owner-sub',
        email: 'owner@example.test',
        emailVerified: true,
        name: 'Owner User',
        pictureUrl: 'https://example.test/owner.png',
        provider: 'Google',
        groups: ['players']
      })
    };

    const moduleRef = await Test.createTestingModule({
      imports: [AppModule]
    })
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

    tokenVerifier.verify.mockResolvedValue({
      sub: 'owner-sub',
      email: 'owner@example.test',
      emailVerified: true,
      name: 'Owner User',
      pictureUrl: 'https://example.test/owner.png',
      provider: 'Google',
      groups: ['players']
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

  it('returns controlled provinces for authenticated wizard clients', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/locations/provinces',
      headers: {
        Authorization: 'Bearer valid-token'
      }
    });

    expect(response.statusCode).toBe(200);
    expect(prismaService.province.findMany).toHaveBeenCalledWith({
      orderBy: { name: 'asc' },
      select: { id: true, code: true, name: true }
    });
    expect(response.json()).toEqual({
      success: true,
      data: [
        {
          id: 'province-id',
          code: 'SJ',
          name: 'San José'
        }
      ],
      errors: [],
      meta: expect.objectContaining({
        path: '/v1/locations/provinces'
      })
    });
  });

  it('returns only cantons that belong to the selected province', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/locations/provinces/province-id/cantons',
      headers: {
        Authorization: 'Bearer valid-token'
      }
    });

    expect(response.statusCode).toBe(400);

    const validProvinceResponse = await app.inject({
      method: 'GET',
      url: '/v1/locations/provinces/3f91fe4d-a23b-4f85-ae1a-90db47d624f1/cantons',
      headers: {
        Authorization: 'Bearer valid-token'
      }
    });

    expect(validProvinceResponse.statusCode).toBe(200);
    expect(prismaService.canton.findMany).toHaveBeenCalledWith({
      where: { provinceId: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1' },
      orderBy: { name: 'asc' },
      select: { id: true, provinceId: true, code: true, name: true }
    });
    expect(validProvinceResponse.json()).toEqual({
      success: true,
      data: [
        {
          id: 'canton-id',
          provinceId: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1',
          code: 'SJ-ESC',
          name: 'Escazú'
        }
      ],
      errors: [],
      meta: expect.objectContaining({
        path: '/v1/locations/provinces/3f91fe4d-a23b-4f85-ae1a-90db47d624f1/cantons'
      })
    });
  });

  it('returns only active services for the requested scope', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/services?scope=COURT',
      headers: {
        Authorization: 'Bearer valid-token'
      }
    });

    expect(response.statusCode).toBe(200);
    expect(prismaService.serviceCatalog.findMany).toHaveBeenCalledWith({
      where: {
        isActive: true,
        scope: 'COURT'
      },
      orderBy: [{ scope: 'asc' }, { name: 'asc' }],
      select: { id: true, name: true, scope: true }
    });
    expect(response.json()).toEqual({
      success: true,
      data: [
        {
          id: 'court-service-id',
          name: 'Lighting',
          scope: 'COURT'
        },
        {
          id: 'grass-service-id',
          name: 'Synthetic Grass',
          scope: 'COURT'
        }
      ],
      errors: [],
      meta: expect.objectContaining({
        path: '/v1/services?scope=COURT'
      })
    });
  });

  it('creates the complex, first court, and service associations from the wizard payload', async () => {
    const state = createTransactionalState();
    const transactionClient = createTransactionClient(state);
    prismaService.$transaction.mockImplementation(async (callback) => callback(transactionClient));

    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: requestPayload
    });

    expect(response.statusCode).toBe(201);
    expect(state.complexServices).toEqual([
      { complexId: 'complex-id', serviceCatalogId: 'd76a5f20-83f0-4538-a1c8-4f7b60d0f4be' }
    ]);
    expect(state.courtServices).toEqual([
      { courtId: 'court-id', serviceCatalogId: 'aab8a9f0-faf2-4e73-a8cb-6853f48cc9a7' },
      { courtId: 'court-id', serviceCatalogId: 'f96c0626-e055-4187-a100-c7d465f51f3b' }
    ]);
    expect(response.json()).toEqual({
      success: true,
      data: {
        complex: {
          id: 'complex-id',
          name: 'North Sports Center',
          provinceId: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1',
          cantonId: '1f6adf24-ea42-4c49-9179-c5f73fef7a41',
          address: '123 Main Street',
          latitude: 9.935,
          longitude: -84.091,
          serviceIds: ['d76a5f20-83f0-4538-a1c8-4f7b60d0f4be'],
          status: 'ACTIVE',
          createdAt: '2026-06-20T00:00:00.000Z',
          updatedAt: '2026-06-20T00:00:00.000Z'
        },
        firstCourt: {
          id: 'court-id',
          complexId: 'complex-id',
          name: 'Court A',
          serviceIds: [
            'aab8a9f0-faf2-4e73-a8cb-6853f48cc9a7',
            'f96c0626-e055-4187-a100-c7d465f51f3b'
          ],
          status: 'ACTIVE',
          createdAt: '2026-06-20T00:00:00.000Z',
          updatedAt: '2026-06-20T00:00:00.000Z'
        }
      },
      errors: [],
      meta: expect.objectContaining({
        path: '/v1/complexes'
      })
    });
  });

  it('rejects a canton that does not belong to the selected province', async () => {
    const transactionClient = createTransactionClient(createTransactionalState(), {
      cantonMatchesProvince: false
    });
    prismaService.$transaction.mockImplementation(async (callback) => callback(transactionClient));

    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: requestPayload
    });

    expect(response.statusCode).toBe(400);
    expect(transactionClient.userIdentity.findUnique).not.toHaveBeenCalled();
    expect(transactionClient.user.create).not.toHaveBeenCalled();
    expect(transactionClient.userRole.upsert).not.toHaveBeenCalled();
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

  it('rejects service ids that are inactive or from the wrong scope', async () => {
    const transactionClient = createTransactionClient(createTransactionalState(), {
      complexServicesFound: false
    });
    prismaService.$transaction.mockImplementation(async (callback) => callback(transactionClient));

    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: requestPayload
    });

    expect(response.statusCode).toBe(400);
    expect(transactionClient.userIdentity.findUnique).not.toHaveBeenCalled();
    expect(transactionClient.user.create).not.toHaveBeenCalled();
    expect(transactionClient.userRole.upsert).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            status: 400,
            message:
              'Selected complex services must exist, be active, and belong to scope COMPLEX.'
          })
        ]
      })
    );
  });

  function createPrismaMock() {
    return {
      $transaction: jest.fn(),
      province: {
        findMany: jest.fn().mockResolvedValue([
          {
            id: 'province-id',
            code: 'SJ',
            name: 'San José'
          }
        ])
      },
      canton: {
        findMany: jest.fn().mockResolvedValue([
          {
            id: 'canton-id',
            provinceId: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1',
            code: 'SJ-ESC',
            name: 'Escazú'
          }
        ])
      },
      serviceCatalog: {
        findMany: jest
          .fn()
          .mockImplementation(async ({ where }: { where: { scope?: string } }) => {
            if (where.scope === 'COURT') {
              return [
                {
                  id: 'court-service-id',
                  name: 'Lighting',
                  scope: 'COURT'
                },
                {
                  id: 'grass-service-id',
                  name: 'Synthetic Grass',
                  scope: 'COURT'
                }
              ];
            }

            return [
              {
                id: 'complex-service-id',
                name: 'Parking',
                scope: 'COMPLEX'
              }
            ];
          })
      },
      onModuleInit: jest.fn(),
      onModuleDestroy: jest.fn()
    };
  }

  function createTransactionalState() {
    return {
      complexServices: [] as Array<Record<string, string>>,
      courtServices: [] as Array<Record<string, string>>
    };
  }

  function createTransactionClient(
    state: ReturnType<typeof createTransactionalState>,
    options?: {
      cantonMatchesProvince?: boolean;
      complexServicesFound?: boolean;
    }
  ) {
    return {
      province: {
        findUnique: jest.fn().mockResolvedValue({ id: requestPayload.complex.provinceId })
      },
      canton: {
        findFirst: jest.fn().mockResolvedValue(
          options?.cantonMatchesProvince === false
            ? null
            : { id: requestPayload.complex.cantonId }
        )
      },
      serviceCatalog: {
        findMany: jest
          .fn()
          .mockImplementation(async ({ where }: { where: { scope: string } }) => {
            if (where.scope === 'COMPLEX') {
              return options?.complexServicesFound === false
                ? []
                : [{ id: 'd76a5f20-83f0-4538-a1c8-4f7b60d0f4be' }];
            }

            return [
              { id: 'aab8a9f0-faf2-4e73-a8cb-6853f48cc9a7' },
              { id: 'f96c0626-e055-4187-a100-c7d465f51f3b' }
            ];
          })
      },
      user: {
        findUnique: jest.fn().mockResolvedValue(null),
        update: jest.fn().mockResolvedValue({ id: 'owner-id' }),
        create: jest.fn().mockResolvedValue({ id: 'owner-id' })
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
      },
      userRole: {
        upsert: jest.fn().mockResolvedValue({ id: 'owner-role-id' })
      },
      complex: {
        create: jest.fn().mockResolvedValue({
          id: 'complex-id',
          name: 'North Sports Center',
          provinceId: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1',
          cantonId: '1f6adf24-ea42-4c49-9179-c5f73fef7a41',
          address: '123 Main Street',
          latitude: 9.935,
          longitude: -84.091,
          status: 'ACTIVE',
          createdAt: new Date('2026-06-20T00:00:00.000Z'),
          updatedAt: new Date('2026-06-20T00:00:00.000Z')
        })
      },
      court: {
        create: jest.fn().mockResolvedValue({
          id: 'court-id',
          complexId: 'complex-id',
          name: 'Court A',
          status: 'ACTIVE',
          createdAt: new Date('2026-06-20T00:00:00.000Z'),
          updatedAt: new Date('2026-06-20T00:00:00.000Z')
        })
      },
      complexService: {
        createMany: jest.fn().mockImplementation(async ({ data }: { data: Array<Record<string, string>> }) => {
          state.complexServices.push(...data);
          return { count: data.length };
        })
      },
      courtService: {
        createMany: jest.fn().mockImplementation(async ({ data }: { data: Array<Record<string, string>> }) => {
          state.courtServices.push(...data);
          return { count: data.length };
        })
      }
    };
  }
});
