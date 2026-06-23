import type { NestFastifyApplication } from '@nestjs/platform-fastify';
import { FastifyAdapter } from '@nestjs/platform-fastify';
import { Test } from '@nestjs/testing';
import type { ITokenVerifierPort } from '@/modules/auth/application/ports/token-verifier.port';
import { TOKEN_VERIFIER_PORT } from '@/modules/auth/application/ports/token-verifier.port';
import { configureValidation } from '@/bootstrap/validation';
import { APP_ERROR_CODES } from '@/shared/domain/errors/app-error-code';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

describe('POST /complexes HTTP contract', () => {
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
    prismaService.$transaction.mockImplementation(async (callback) =>
      callback({
        user: {
          create: jest.fn().mockResolvedValue({ id: 'owner-id' }),
          findUnique: jest.fn().mockResolvedValue(null),
          update: jest.fn()
        },
        userIdentity: {
          findUnique: jest.fn().mockResolvedValue(null)
        },
        userRole: {
          findUnique: jest.fn().mockResolvedValue({ id: 'owner-role-id' }),
          upsert: jest.fn().mockResolvedValue({ id: 'owner-role-id' }),
          deleteMany: jest.fn().mockResolvedValue({ count: 0 })
        },
        complex: {
          create: jest.fn().mockResolvedValue({
            id: 'complex-id',
            name: 'North Sports Center',
            address: '123 Main Street',
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
        }
      })
    );
  }

  function mockForbiddenTransaction(): void {
    prismaService.$transaction.mockImplementation(async (callback) =>
      callback({
        user: {
          create: jest.fn().mockResolvedValue({ id: 'owner-id' }),
          findUnique: jest.fn().mockResolvedValue(null),
          update: jest.fn()
        },
        userIdentity: {
          findUnique: jest.fn().mockResolvedValue(null)
        },
        userRole: {
          findUnique: jest.fn().mockResolvedValue(null),
          upsert: jest.fn(),
          deleteMany: jest.fn()
        },
        complex: {
          create: jest.fn()
        },
        court: {
          create: jest.fn()
        }
      })
    );
  }

  it('returns 201 with the standard success envelope when the owner already has a persisted OWNER role', async () => {
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

  it('returns 403 when the authenticated user is not an OWNER', async () => {
    mockForbiddenTransaction();

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

    expect(response.statusCode).toBe(403);
    expect(response.json()).toEqual({
      success: false,
      data: null,
      errors: [
        {
          code: APP_ERROR_CODES.FORBIDDEN,
          message: 'Only users with the OWNER role can create complexes.',
          status: 403,
          type: 'urn:problem-type:backend:forbidden'
        }
      ],
      meta: {
        requestId: expect.any(String),
        path: '/v1/complexes',
        timestamp: expect.any(String)
      }
    });
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

  it('returns 403 when the demo email allowlist matches but email is not verified', async () => {
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
    mockForbiddenTransaction();

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

    expect(response.statusCode).toBe(403);
  });

  it('returns 403 when the demo email allowlist matches but email verification is missing', async () => {
    process.env.DEMO_OWNER_EMAILS = 'owner@example.test';
    tokenVerifier.verify.mockResolvedValue({
      sub: 'owner-sub',
      email: 'owner@example.test',
      name: 'Owner User',
      pictureUrl: 'https://example.test/owner.png',
      provider: 'Google',
      groups: ['players']
    });
    mockForbiddenTransaction();

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

    expect(response.statusCode).toBe(403);
  });
});
