import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { FastifyAdapter } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';
import type { ITokenVerifierPort } from '@/modules/auth/application/ports/token-verifier.port';
import { TOKEN_VERIFIER_PORT } from '@/modules/auth/application/ports/token-verifier.port';
import { configureValidation } from '@/bootstrap/validation';
import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
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

  const addCourtPayload = {
    name: 'Court B',
    serviceIds: [
      'aab8a9f0-faf2-4e73-a8cb-6853f48cc9a7',
      'f96c0626-e055-4187-a100-c7d465f51f3b'
    ]
  };
  const ownedComplexId = '7b9d0b87-9e3d-4b43-b5a7-50f79d82fd40';
  const validCourtImageUploadId = '9f6b4f0f-5f5a-4d8d-8c5e-2b2e7b0f6a3c';
  const wrongOwnerImageUploadId = '7a4d9ef5-6be0-47b4-a726-2c6df8a64ce4';
  const wrongPurposeImageUploadId = 'c5d03ef0-8efc-4d06-a8fb-48ae624e28c2';
  const unknownImageUploadId = '86ea60ec-08c1-48ae-bc64-f63b7a991d9c';
  const malformedImageUploadId = 'not-a-uuid';
  const alreadyAssignedImageUploadId = '6f554321-6df0-43c4-b310-f3d7e6bf00a1';

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

  it('returns the authenticated owner my complex hub in the standard envelope', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/complexes/my-hub',
      headers: {
        Authorization: 'Bearer valid-token'
      }
    });

    expect(response.statusCode).toBe(200);
    expect(prismaService.complex.findMany).toHaveBeenCalledWith({
      where: {
        deletedAt: null,
        owner: {
          identities: {
            some: {
              provider: 'Google',
              providerSubject: 'owner-sub'
            }
          }
        }
      },
      orderBy: [{ createdAt: 'asc' }, { name: 'asc' }],
      select: {
        id: true,
        name: true,
        address: true,
        provinceId: true,
        cantonId: true,
        latitude: true,
        longitude: true,
        status: true,
        courts: {
          where: { deletedAt: null },
          orderBy: [{ createdAt: 'asc' }, { name: 'asc' }],
          select: {
            id: true,
            name: true,
            status: true,
            availability: {
              select: {
                id: true
              }
            }
          }
        }
      }
    });
    expect(response.json()).toEqual({
      success: true,
      data: {
        complexes: [
          {
            id: 'complex-id',
            name: 'North Sports Center',
            address: '123 Main Street',
            provinceId: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1',
            cantonId: '1f6adf24-ea42-4c49-9179-c5f73fef7a41',
            latitude: 9.935,
            longitude: -84.091,
            status: 'ACTIVE',
            courts: [
              {
                id: 'court-configured-id',
                name: 'Court A',
                status: 'ACTIVE',
                availabilityStatus: 'CONFIGURED'
              },
              {
                id: 'court-pending-id',
                name: 'Court B',
                status: 'ACTIVE',
                availabilityStatus: 'PENDING'
              }
            ]
          }
        ]
      },
      errors: [],
      meta: expect.objectContaining({
        path: '/v1/complexes/my-hub'
      })
    });
  });

  it('rejects unauthenticated my complex hub requests with the standard error envelope', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/v1/complexes/my-hub'
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
        path: '/v1/complexes/my-hub',
        timestamp: expect.any(String)
      }
    });
  });

  it('creates a court for an owned complex in the standard envelope', async () => {
    const state = createTransactionalState();
    const transactionClient = createTransactionClient(state);
    prismaService.$transaction.mockImplementation(async (callback) => callback(transactionClient));

    const response = await app.inject({
      method: 'POST',
      url: `/v1/complexes/${ownedComplexId}/courts`,
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: addCourtPayload
    });

    expect(response.statusCode).toBe(201);
    expect(prismaService.imageUpload.findUnique).not.toHaveBeenCalled();
    expect(transactionClient.complex.findFirst).toHaveBeenCalledWith({
      where: {
        id: ownedComplexId,
        deletedAt: null,
        owner: {
          identities: {
            some: {
              provider: 'Google',
              providerSubject: 'owner-sub'
            }
          }
        }
      },
      select: { id: true }
    });
    expect(state.courtServices).toEqual([
      { courtId: 'court-id', serviceCatalogId: 'aab8a9f0-faf2-4e73-a8cb-6853f48cc9a7' },
      { courtId: 'court-id', serviceCatalogId: 'f96c0626-e055-4187-a100-c7d465f51f3b' }
    ]);
    expect(response.json()).toEqual({
      success: true,
      data: {
        court: {
          id: 'court-id',
          complexId: ownedComplexId,
          name: 'Court B',
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
        path: `/v1/complexes/${ownedComplexId}/courts`
      })
    });
  });

  it('accepts and forwards a confirmed court image upload when adding a court', async () => {
    const state = createTransactionalState();
    const transactionClient = createTransactionClient(state);
    prismaService.$transaction.mockImplementation(async (callback) => callback(transactionClient));

    const response = await app.inject({
      method: 'POST',
      url: `/v1/complexes/${ownedComplexId}/courts`,
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: {
        ...addCourtPayload,
        imageUploadId: validCourtImageUploadId
      }
    });

    expect(response.statusCode).toBe(201);
    expect(prismaService.imageUpload.findUnique).toHaveBeenCalledWith({
      where: { id: validCourtImageUploadId }
    });
    expect(transactionClient.court.create).toHaveBeenCalledWith({
      data: expect.objectContaining({
        complexId: ownedComplexId,
        name: addCourtPayload.name,
        imageUploadId: validCourtImageUploadId
      })
    });
  });

  it('rejects unauthenticated add court requests with the standard error envelope', async () => {
    const response = await app.inject({
      method: 'POST',
      url: `/v1/complexes/${ownedComplexId}/courts`,
      payload: addCourtPayload
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
        path: `/v1/complexes/${ownedComplexId}/courts`,
        timestamp: expect.any(String)
      }
    });
  });

  it('rejects add court when the complex is not owned by the authenticated user', async () => {
    const state = createTransactionalState();
    const transactionClient = createTransactionClient(state, { ownedComplexFound: false });
    prismaService.$transaction.mockImplementation(async (callback) => callback(transactionClient));

    const response = await app.inject({
      method: 'POST',
      url: `/v1/complexes/${ownedComplexId}/courts`,
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: addCourtPayload
    });

    expect(response.statusCode).toBe(404);
    expect(transactionClient.court.create).not.toHaveBeenCalled();
    expect(transactionClient.courtService.createMany).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.RESOURCE_NOT_FOUND,
            status: 404,
            message: 'Complex not found for the authenticated owner.'
          })
        ],
        meta: expect.objectContaining({
          path: `/v1/complexes/${ownedComplexId}/courts`
        })
      })
    );
  });

  it('rejects add court when the selected court services are inactive or from the wrong scope', async () => {
    const state = createTransactionalState();
    const transactionClient = createTransactionClient(state, { courtServicesFound: false });
    prismaService.$transaction.mockImplementation(async (callback) => callback(transactionClient));

    const response = await app.inject({
      method: 'POST',
      url: `/v1/complexes/${ownedComplexId}/courts`,
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: addCourtPayload
    });

    expect(response.statusCode).toBe(400);
    expect(transactionClient.court.create).not.toHaveBeenCalled();
    expect(transactionClient.courtService.createMany).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            status: 400,
            message: 'Selected court services must exist, be active, and belong to scope COURT.'
          })
        ],
        meta: expect.objectContaining({
          path: `/v1/complexes/${ownedComplexId}/courts`
        })
      })
    );
  });

  it('rejects add court payloads that fail HTTP validation before opening a transaction', async () => {
    const response = await app.inject({
      method: 'POST',
      url: `/v1/complexes/${ownedComplexId}/courts`,
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: {
        ...addCourtPayload,
        serviceIds: []
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
          path: `/v1/complexes/${ownedComplexId}/courts`
        })
      })
    );
    expect(prismaService.$transaction).not.toHaveBeenCalled();
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
    expect(prismaService.imageUpload.findUnique).not.toHaveBeenCalled();
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

  it('accepts and forwards a confirmed court image upload when creating a complex', async () => {
    const state = createTransactionalState();
    const transactionClient = createTransactionClient(state);
    prismaService.$transaction.mockImplementation(async (callback) => callback(transactionClient));

    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: {
        ...requestPayload,
        firstCourt: {
          ...requestPayload.firstCourt,
          imageUploadId: validCourtImageUploadId
        }
      }
    });

    expect(response.statusCode).toBe(201);
    expect(prismaService.imageUpload.findUnique).toHaveBeenCalledWith({
      where: { id: validCourtImageUploadId }
    });
    expect(transactionClient.court.create).toHaveBeenCalledWith({
      data: expect.objectContaining({
        complexId: 'complex-id',
        name: requestPayload.firstCourt.name,
        imageUploadId: validCourtImageUploadId
      })
    });
  });

  it.each([
    {
      name: 'rejects a malformed court image upload id while creating a complex',
      url: '/v1/complexes',
      payload: {
        ...requestPayload,
        firstCourt: {
          ...requestPayload.firstCourt,
          imageUploadId: malformedImageUploadId
        }
      },
      path: '/v1/complexes'
    },
    {
      name: 'rejects a malformed court image upload id while adding a court',
      url: `/v1/complexes/${ownedComplexId}/courts`,
      payload: {
        ...addCourtPayload,
        imageUploadId: malformedImageUploadId
      },
      path: `/v1/complexes/${ownedComplexId}/courts`
    }
  ])('$name', async ({ url, payload, path }) => {
    const response = await app.inject({
      method: 'POST',
      url,
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.imageUpload.findUnique).not.toHaveBeenCalled();
    expect(prismaService.$transaction).not.toHaveBeenCalled();
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
        meta: expect.objectContaining({ path })
      })
    );
  });

  it.each([
    {
      name: 'rejects an unknown court image upload while creating a complex',
      url: '/v1/complexes',
      payload: {
        ...requestPayload,
        firstCourt: {
          ...requestPayload.firstCourt,
          imageUploadId: unknownImageUploadId
        }
      },
      path: '/v1/complexes'
    },
    {
      name: 'rejects an unknown court image upload while adding a court',
      url: `/v1/complexes/${ownedComplexId}/courts`,
      payload: {
        ...addCourtPayload,
        imageUploadId: unknownImageUploadId
      },
      path: `/v1/complexes/${ownedComplexId}/courts`
    }
  ])('$name', async ({ url, payload, path }) => {
    const response = await app.inject({
      method: 'POST',
      url,
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.$transaction).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            status: 400,
            message:
              'La imagen seleccionada para la cancha debe ser una carga confirmada del usuario autenticado.'
          })
        ],
        meta: expect.objectContaining({ path })
      })
    );
  });

  it.each([
    {
      name: 'rejects a reused court image upload while creating a complex',
      url: '/v1/complexes',
      payload: {
        ...requestPayload,
        firstCourt: {
          ...requestPayload.firstCourt,
          imageUploadId: alreadyAssignedImageUploadId
        }
      },
      path: '/v1/complexes'
    },
    {
      name: 'rejects a reused court image upload while adding a court',
      url: `/v1/complexes/${ownedComplexId}/courts`,
      payload: {
        ...addCourtPayload,
        imageUploadId: alreadyAssignedImageUploadId
      },
      path: `/v1/complexes/${ownedComplexId}/courts`
    }
  ])('$name', async ({ url, payload, path }) => {
    const response = await app.inject({
      method: 'POST',
      url,
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.$transaction).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            status: 400,
            message:
              'La imagen seleccionada para la cancha debe ser una carga confirmada del usuario autenticado.'
          })
        ],
        meta: expect.objectContaining({ path })
      })
    );
  });

  it.each([
    {
      name: 'rejects another owner court image upload while creating a complex',
      url: '/v1/complexes',
      payload: {
        ...requestPayload,
        firstCourt: {
          ...requestPayload.firstCourt,
          imageUploadId: wrongOwnerImageUploadId
        }
      },
      path: '/v1/complexes'
    },
    {
      name: 'rejects another owner court image upload while adding a court',
      url: `/v1/complexes/${ownedComplexId}/courts`,
      payload: {
        ...addCourtPayload,
        imageUploadId: wrongOwnerImageUploadId
      },
      path: `/v1/complexes/${ownedComplexId}/courts`
    }
  ])('$name', async ({ url, payload, path }) => {
    const response = await app.inject({
      method: 'POST',
      url,
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.$transaction).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            status: 400,
            message:
              'La imagen seleccionada para la cancha debe ser una carga confirmada del usuario autenticado.'
          })
        ],
        meta: expect.objectContaining({ path })
      })
    );
  });

  it.each([
    {
      name: 'rejects a non court-image upload while creating a complex',
      url: '/v1/complexes',
      payload: {
        ...requestPayload,
        firstCourt: {
          ...requestPayload.firstCourt,
          imageUploadId: wrongPurposeImageUploadId
        }
      },
      path: '/v1/complexes'
    },
    {
      name: 'rejects a non court-image upload while adding a court',
      url: `/v1/complexes/${ownedComplexId}/courts`,
      payload: {
        ...addCourtPayload,
        imageUploadId: wrongPurposeImageUploadId
      },
      path: `/v1/complexes/${ownedComplexId}/courts`
    }
  ])('$name', async ({ url, payload, path }) => {
    const response = await app.inject({
      method: 'POST',
      url,
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.$transaction).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            status: 400,
            message:
              'La imagen seleccionada para la cancha debe ser una carga confirmada del usuario autenticado.'
          })
        ],
        meta: expect.objectContaining({ path })
      })
    );
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

  it('rejects an empty first court service selection at the HTTP boundary', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: {
        ...requestPayload,
        firstCourt: {
          ...requestPayload.firstCourt,
          serviceIds: []
        }
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
          path: '/v1/complexes'
        })
      })
    );
    expect(prismaService.$transaction).not.toHaveBeenCalled();
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
      complex: {
        findMany: jest.fn().mockResolvedValue([
          {
            id: 'complex-id',
            name: 'North Sports Center',
            address: '123 Main Street',
            provinceId: '3f91fe4d-a23b-4f85-ae1a-90db47d624f1',
            cantonId: '1f6adf24-ea42-4c49-9179-c5f73fef7a41',
            latitude: 9.935,
            longitude: -84.091,
            status: 'ACTIVE',
            courts: [
              {
                id: 'court-configured-id',
                name: 'Court A',
                status: 'ACTIVE',
                availability: { id: 'availability-id' }
              },
              {
                id: 'court-pending-id',
                name: 'Court B',
                status: 'ACTIVE',
                availability: null
              }
            ]
          }
        ])
      },
      court: {
        findFirst: jest.fn().mockImplementation(async ({ where }: { where: { imageUploadId?: string } }) => {
          if (where.imageUploadId === alreadyAssignedImageUploadId) {
            return { id: 'existing-court-id' };
          }

          return null;
        })
      },
      imageUpload: {
        findUnique: jest.fn().mockImplementation(async ({ where }: { where: { id: string } }) => {
          if (where.id === validCourtImageUploadId) {
            return createImageUploadRecord({
              id: validCourtImageUploadId,
              ownerSub: 'owner-sub',
              purpose: FilePurpose.CourtImage
            });
          }

          if (where.id === wrongOwnerImageUploadId) {
            return createImageUploadRecord({
              id: wrongOwnerImageUploadId,
              ownerSub: 'other-owner-sub',
              purpose: FilePurpose.CourtImage
            });
          }

          if (where.id === wrongPurposeImageUploadId) {
            return createImageUploadRecord({
              id: wrongPurposeImageUploadId,
              ownerSub: 'owner-sub',
              purpose: FilePurpose.ProfileImage
            });
          }

          if (where.id === alreadyAssignedImageUploadId) {
            return createImageUploadRecord({
              id: alreadyAssignedImageUploadId,
              ownerSub: 'owner-sub',
              purpose: FilePurpose.CourtImage
            });
          }

          return null;
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
      courtServicesFound?: boolean;
      ownedComplexFound?: boolean;
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

            return options?.courtServicesFound === false
              ? [{ id: 'aab8a9f0-faf2-4e73-a8cb-6853f48cc9a7' }]
              : [
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
        findFirst: jest.fn().mockResolvedValue(
          options?.ownedComplexFound === false ? null : { id: ownedComplexId }
        ),
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
        findFirst: jest.fn().mockImplementation(async ({ where }: { where: { imageUploadId?: string } }) => {
          if (where.imageUploadId === alreadyAssignedImageUploadId) {
            return { id: 'existing-court-id' };
          }

          return null;
        }),
        create: jest.fn().mockImplementation(async ({ data }: { data: { complexId: string; name: string } }) => ({
          id: 'court-id',
          complexId: data.complexId,
          name: data.name,
          status: 'ACTIVE',
          createdAt: new Date('2026-06-20T00:00:00.000Z'),
          updatedAt: new Date('2026-06-20T00:00:00.000Z')
        }))
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

  function createImageUploadRecord(input: {
    id: string;
    ownerSub: string;
    purpose: FilePurpose;
  }) {
    return {
      id: input.id,
      ownerSub: input.ownerSub,
      ownerEmail: 'owner@example.test',
      ownerName: 'Owner User',
      ownerPictureUrl: 'https://example.test/owner.png',
      ownerProvider: 'Google',
      purpose: input.purpose,
      objectKey: `test/uploads/${input.purpose}/${input.ownerSub}/2026/06/file.jpg`,
      contentType: 'image/jpeg',
      sizeBytes: 512,
      createdAt: new Date('2026-06-20T00:00:00.000Z')
    };
  }
});
