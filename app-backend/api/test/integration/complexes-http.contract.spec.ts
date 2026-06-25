import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { FastifyAdapter } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';
import type { ITokenVerifierPort } from '@/modules/auth/application/ports/token-verifier.port';
import { TOKEN_VERIFIER_PORT } from '@/modules/auth/application/ports/token-verifier.port';
import { configureValidation } from '@/bootstrap/validation';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

describe('POST /complexes HTTP contract', () => {
  interface ITransactionalState {
    userIds: string[];
    ownerRoles: Array<{ userId: string; role: 'OWNER' }>;
    complexes: Array<{ id: string; ownerId: string; name: string; address: string }>;
    courts: Array<{ id: string; complexId: string; name: string }>;
  }

  const originalEnv = {
    DATABASE_URL: process.env.DATABASE_URL,
    AWS_REGION: process.env.AWS_REGION,
    COGNITO_USER_POOL_ID: process.env.COGNITO_USER_POOL_ID,
    COGNITO_CLIENT_ID: process.env.COGNITO_CLIENT_ID,
    APP_S3_BUCKET_NAME: process.env.APP_S3_BUCKET_NAME,
    APP_S3_KEY_PREFIX: process.env.APP_S3_KEY_PREFIX,
    APP_S3_PROFILE_IMAGE_MAX_BYTES: process.env.APP_S3_PROFILE_IMAGE_MAX_BYTES,
    APP_S3_ALLOWED_IMAGE_MIME_TYPES: process.env.APP_S3_ALLOWED_IMAGE_MIME_TYPES,
    WEBSOCKET_CONNECTIONS_TABLE_NAME: process.env.WEBSOCKET_CONNECTIONS_TABLE_NAME,
    DEMO_OWNER_EMAILS: process.env.DEMO_OWNER_EMAILS,
    DEMO_OWNER_SUBS: process.env.DEMO_OWNER_SUBS
  };

  let app: NestFastifyApplication;
  let prismaService: {
    $transaction: jest.Mock;
    user: {
      create: jest.Mock;
      findUnique: jest.Mock;
      update: jest.Mock;
    };
    userIdentity: { findUnique: jest.Mock };
    onModuleInit: jest.Mock;
    onModuleDestroy: jest.Mock;
  };
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

    prismaService = {
      $transaction: jest.fn(),
      user: {
        create: jest.fn(),
        findUnique: jest.fn(),
        update: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn()
      },
      onModuleInit: jest.fn(),
      onModuleDestroy: jest.fn()
    };
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
    delete process.env.DEMO_OWNER_EMAILS;
    delete process.env.DEMO_OWNER_SUBS;

    prismaService.user.create.mockResolvedValue({ id: 'owner-id' });
    prismaService.user.findUnique.mockResolvedValue(null);
    prismaService.user.update.mockResolvedValue({ id: 'owner-id' });
    prismaService.userIdentity.findUnique.mockResolvedValue(null);

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
        continue;
      }

      process.env[key] = value;
    }
  });

  function mockSuccessfulTransaction(): void {
    prismaService.$transaction.mockImplementation(async (callback) => callback(createTransactionClient()));
  }

  function createTransactionalPersistenceHarness(options?: {
    failComplexCreate?: boolean;
  }): {
    state: ITransactionalState;
    rootClient: {
      user: {
        create: jest.Mock;
        findUnique: jest.Mock;
        update: jest.Mock;
      };
      userIdentity: { findUnique: jest.Mock };
      userRole: { upsert: jest.Mock };
      complex: { create: jest.Mock };
      court: { create: jest.Mock };
    };
    transaction: jest.Mock;
  } {
    const state: ITransactionalState = {
      userIds: [],
      ownerRoles: [],
      complexes: [],
      courts: []
    };

    const rootClient = {
      user: {
        create: jest.fn().mockResolvedValue({ id: 'owner-id' }),
        findUnique: jest.fn().mockResolvedValue(null),
        update: jest.fn().mockResolvedValue({ id: 'owner-id' })
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
      },
      userRole: {
        upsert: jest.fn()
      },
      complex: {
        create: jest.fn()
      },
      court: {
        create: jest.fn()
      }
    };

    const cloneState = (current: ITransactionalState): ITransactionalState => ({
      userIds: [...current.userIds],
      ownerRoles: current.ownerRoles.map((role) => ({ ...role })),
      complexes: current.complexes.map((complex) => ({ ...complex })),
      courts: current.courts.map((court) => ({ ...court }))
    });

    const transaction = jest.fn(async (callback: (client: ReturnType<typeof createTransactionClient>) => Promise<unknown>) => {
      const transactionState = cloneState(state);
      const transactionClient = createTransactionClient({
        onUserCreate: () => {
          transactionState.userIds.push('owner-id');
        },
        onOwnerRoleUpsert: () => {
          transactionState.ownerRoles.push({ userId: 'owner-id', role: 'OWNER' });
        },
        onComplexCreate: () => {
          transactionState.complexes.push({
            id: 'complex-id',
            ownerId: 'owner-id',
            name: 'North Sports Center',
            address: '123 Main Street'
          });
        },
        onCourtCreate: () => {
          transactionState.courts.push({
            id: 'court-id',
            complexId: 'complex-id',
            name: 'Court A'
          });
        },
        failComplexCreate: options?.failComplexCreate === true
      });

      try {
        const result = await callback(transactionClient);

        state.userIds = transactionState.userIds;
        state.ownerRoles = transactionState.ownerRoles;
        state.complexes = transactionState.complexes;
        state.courts = transactionState.courts;

        return result;
      } catch (error) {
        return Promise.reject(error);
      }
    });

    return {
      state,
      rootClient,
      transaction
    };
  }

  function createTransactionClient(options?: {
    onUserCreate?: () => void;
    onOwnerRoleUpsert?: () => void;
    onComplexCreate?: () => void;
    onCourtCreate?: () => void;
    failComplexCreate?: boolean;
  }): {
    user: {
      create: jest.Mock;
      findUnique: jest.Mock;
      update: jest.Mock;
    };
    userIdentity: { findUnique: jest.Mock };
    userRole: { upsert: jest.Mock; deleteMany: jest.Mock };
    complex: { create: jest.Mock };
    court: { create: jest.Mock };
  } {
    return {
      user: {
        create: jest.fn().mockImplementation(async () => {
          options?.onUserCreate?.();

          return { id: 'owner-id' };
        }),
        findUnique: jest.fn().mockResolvedValue(null),
        update: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
      },
      userRole: {
        upsert: jest.fn().mockImplementation(async () => {
          options?.onOwnerRoleUpsert?.();

          return { id: 'owner-role-id' };
        }),
        deleteMany: jest.fn().mockResolvedValue({ count: 0 })
      },
      complex: {
        create: jest.fn().mockImplementation(async () => {
          if (options?.failComplexCreate === true) {
            throw new Error('complex-create-failed');
          }

          options?.onComplexCreate?.();

          return {
            id: 'complex-id',
            name: 'North Sports Center',
            address: '123 Main Street',
            status: 'ACTIVE',
            createdAt: new Date('2026-06-20T00:00:00.000Z'),
            updatedAt: new Date('2026-06-20T00:00:00.000Z')
          };
        })
      },
      court: {
        create: jest.fn().mockImplementation(async () => {
          options?.onCourtCreate?.();

          return {
            id: 'court-id',
            complexId: 'complex-id',
            name: 'Court A',
            status: 'ACTIVE',
            createdAt: new Date('2026-06-20T00:00:00.000Z'),
            updatedAt: new Date('2026-06-20T00:00:00.000Z')
          };
        })
      }
    };
  }

  it('returns 201 with the standard success envelope for an authenticated user creating a first complex', async () => {
    const transactionClient = createTransactionClient();
    prismaService.$transaction.mockImplementation(async (callback) => callback(transactionClient));

    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: {
        complex: {
          name: 'North Sports Center',
          address: '123 Main Street'
        },
        firstCourt: {
          name: 'Court A'
        }
      }
    });

    expect(response.statusCode).toBe(201);
    expect(transactionClient.userRole.upsert).toHaveBeenCalledWith({
      where: {
        userId_role: {
          userId: 'owner-id',
          role: 'OWNER'
        }
      },
      create: {
        userId: 'owner-id',
        role: 'OWNER'
      },
      update: {}
    });
    expect(response.json()).toEqual({
      success: true,
      data: {
        complex: {
          id: 'complex-id',
          name: 'North Sports Center',
          address: '123 Main Street',
          status: 'ACTIVE',
          createdAt: '2026-06-20T00:00:00.000Z',
          updatedAt: '2026-06-20T00:00:00.000Z'
        },
        firstCourt: {
          id: 'court-id',
          complexId: 'complex-id',
          name: 'Court A',
          status: 'ACTIVE',
          createdAt: '2026-06-20T00:00:00.000Z',
          updatedAt: '2026-06-20T00:00:00.000Z'
        }
      },
      errors: [],
      meta: {
        requestId: expect.any(String),
        path: '/v1/complexes',
        timestamp: expect.any(String)
      }
    });
    expect(tokenVerifier.verify).toHaveBeenCalledWith('valid-token');
  });

  it.each([
    [{ firstCourt: { name: 'Court A' } }, 'complex'],
    [{ complex: { name: 'North Sports Center', address: '123 Main Street' } }, 'firstCourt']
  ])(
    'returns 400 when %s is missing from the payload',
    async (payload, missingField) => {
      const response = await app.inject({
        method: 'POST',
        url: '/v1/complexes',
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
          errors: expect.arrayContaining([
            expect.objectContaining({
              code: APP_ERROR_CODES.VALIDATION_FAILED,
              status: 400,
              message: expect.stringContaining(missingField)
            })
          ]),
          meta: expect.objectContaining({
            path: '/v1/complexes'
          })
        })
      );
    }
  );

  it('returns 400 when visible field lengths exceed DTO limits', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: {
        complex: {
          name: 'N'.repeat(121),
          address: '123 Main Street'
        },
        firstCourt: {
          name: 'Court A'
        }
      }
    });

    expect(response.statusCode).toBe(400);
    expect(prismaService.$transaction).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        errors: [
          expect.objectContaining({
            code: APP_ERROR_CODES.VALIDATION_FAILED,
            status: 400,
            message: 'complex.name must be shorter than or equal to 120 characters'
          })
        ]
      })
    );
  });

  it('preserves existing local roles while adding OWNER for the authenticated user', async () => {
    const transactionClient = createTransactionClient();
    prismaService.$transaction.mockImplementation(async (callback) => callback(transactionClient));

    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: {
        complex: {
          name: 'North Sports Center',
          address: '123 Main Street'
        },
        firstCourt: {
          name: 'Court A'
        }
      }
    });

    expect(response.statusCode).toBe(201);
    expect(transactionClient.userRole.deleteMany).not.toHaveBeenCalled();
  });

  it('returns 201 for an allowlisted demo owner matched by Cognito sub', async () => {
    process.env.DEMO_OWNER_SUBS = 'owner-sub';
    mockSuccessfulTransaction();

    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: {
        complex: {
          name: 'North Sports Center',
          address: '123 Main Street'
        },
        firstCourt: {
          name: 'Court A'
        }
      }
    });

    expect(response.statusCode).toBe(201);
  });

  it('returns 201 for a user who is already an OWNER without creating duplicate OWNER assignments', async () => {
    const transactionClient = createTransactionClient();
    prismaService.$transaction.mockImplementation(async (callback) => callback(transactionClient));

    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: {
        complex: {
          name: 'North Sports Center',
          address: '123 Main Street'
        },
        firstCourt: {
          name: 'Court A'
        }
      }
    });

    expect(response.statusCode).toBe(201);
    expect(transactionClient.userRole.upsert).toHaveBeenCalledTimes(1);
  });

  it('returns 401 when the request is unauthenticated', async () => {
    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      payload: {
        complex: {
          name: 'North Sports Center',
          address: '123 Main Street'
        },
        firstCourt: {
          name: 'Court A'
        }
      }
    });

    expect(response.statusCode).toBe(401);
    expect(prismaService.$transaction).not.toHaveBeenCalled();
    expect(response.json()).toEqual(
      expect.objectContaining({
        success: false,
        data: null,
        errors: expect.arrayContaining([
          expect.objectContaining({
            code: APP_ERROR_CODES.AUTH_MISSING_TOKEN,
            status: 401,
            message: 'Authentication token is required.'
          })
        ]),
        meta: expect.objectContaining({
          path: '/v1/complexes'
        })
      })
    );
  });

  it('returns 500 when the transaction fails after OWNER upsert so no OWNER role, complex, or court is persisted', async () => {
    const harness = createTransactionalPersistenceHarness({ failComplexCreate: true });
    prismaService.user = harness.rootClient.user;
    prismaService.userIdentity = harness.rootClient.userIdentity;
    prismaService.$transaction.mockImplementation(harness.transaction);

    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: {
        complex: {
          name: 'North Sports Center',
          address: '123 Main Street'
        },
        firstCourt: {
          name: 'Court A'
        }
      }
    });

    expect(response.statusCode).toBe(500);
    expect(harness.transaction).toHaveBeenCalledTimes(1);
    expect(harness.state.ownerRoles).toEqual([]);
    expect(harness.state.complexes).toEqual([]);
    expect(harness.state.courts).toEqual([]);
    expect(harness.rootClient.userRole.upsert).not.toHaveBeenCalled();
    expect(harness.rootClient.complex.create).not.toHaveBeenCalled();
    expect(harness.rootClient.court.create).not.toHaveBeenCalled();
  });

  it('returns 201 for an allowlisted demo owner matched by verified email', async () => {
    process.env.DEMO_OWNER_EMAILS = 'owner@example.test';
    mockSuccessfulTransaction();

    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: {
        complex: {
          name: 'North Sports Center',
          address: '123 Main Street'
        },
        firstCourt: {
          name: 'Court A'
        }
      }
    });

    expect(response.statusCode).toBe(201);
  });

  it('returns 201 when the demo email allowlist matches but email is not verified because onboarding no longer depends on allowlist eligibility', async () => {
    process.env.DEMO_OWNER_EMAILS = 'owner@example.test';
    tokenVerifier.verify.mockResolvedValue({
      sub: 'owner-sub',
      email: 'owner@example.test',
      emailVerified: false,
      name: 'Owner User',
      pictureUrl: 'https://example.test/owner.png',
      provider: 'Google',
      groups: ['players']
    });
    const transactionClient = createTransactionClient();
    prismaService.$transaction.mockImplementation(async (callback) => callback(transactionClient));

    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: {
        complex: {
          name: 'North Sports Center',
          address: '123 Main Street'
        },
        firstCourt: {
          name: 'Court A'
        }
      }
    });

    expect(response.statusCode).toBe(201);
  });

  it('returns 201 when the demo email allowlist matches but email verification is missing because onboarding no longer depends on allowlist eligibility', async () => {
    process.env.DEMO_OWNER_EMAILS = 'owner@example.test';
    tokenVerifier.verify.mockResolvedValue({
      sub: 'owner-sub',
      email: 'owner@example.test',
      name: 'Owner User',
      pictureUrl: 'https://example.test/owner.png',
      provider: 'Google',
      groups: ['players']
    });
    const transactionClient = createTransactionClient();
    prismaService.$transaction.mockImplementation(async (callback) => callback(transactionClient));

    const response = await app.inject({
      method: 'POST',
      url: '/v1/complexes',
      headers: {
        Authorization: 'Bearer valid-token'
      },
      payload: {
        complex: {
          name: 'North Sports Center',
          address: '123 Main Street'
        },
        firstCourt: {
          name: 'Court A'
        }
      }
    });

    expect(response.statusCode).toBe(201);
  });
});
