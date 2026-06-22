import { CreateComplexWithFirstCourtUseCase } from '@/modules/complexes/application/use-cases/create-complex-with-first-court.use-case';
import type {
  IComplexRepository,
  ICreateComplexWithFirstCourtResult
} from '@/modules/complexes/domain/repositories/complex.repository';
import { OwnerRoleRequiredError } from '@/modules/complexes/domain/errors/owner-role-required.error';
import { PrismaComplexRepository } from '@/modules/complexes/infrastructure/persistence/prisma-complex.repository';
import { ComplexesController } from '@/modules/complexes/interfaces/http/controllers/complexes.controller';

describe('complexes module behavior', () => {
  const originalDemoOwnerEmails = process.env.DEMO_OWNER_EMAILS;
  const originalDemoOwnerSubs = process.env.DEMO_OWNER_SUBS;

  afterEach(() => {
    if (originalDemoOwnerEmails === undefined) {
      delete process.env.DEMO_OWNER_EMAILS;
    } else {
      process.env.DEMO_OWNER_EMAILS = originalDemoOwnerEmails;
    }

    if (originalDemoOwnerSubs === undefined) {
      delete process.env.DEMO_OWNER_SUBS;
    } else {
      process.env.DEMO_OWNER_SUBS = originalDemoOwnerSubs;
    }
  });

  const authenticatedUser = {
    sub: 'owner-sub',
    email: 'owner@example.test',
    emailVerified: true,
    name: 'Owner User',
    pictureUrl: 'https://example.test/owner.png',
    provider: 'Google',
    groups: ['players']
  };

  const request = {
    complex: {
      name: 'North Sports Center',
      address: '123 Main Street'
    },
    firstCourt: {
      name: 'Court A'
    }
  };

  const created = {
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
  } satisfies ICreateComplexWithFirstCourtResult;

  it('creates a complex with its first court through the repository port', async () => {
    const repository = {
      createComplexWithFirstCourt: jest.fn().mockResolvedValue(created)
    } satisfies IComplexRepository;
    const useCase = new CreateComplexWithFirstCourtUseCase(repository);

    await expect(useCase.execute(authenticatedUser, request)).resolves.toEqual(created);
    expect(repository.createComplexWithFirstCourt).toHaveBeenCalledWith({
      ownerIdentity: {
        sub: 'owner-sub',
        email: 'owner@example.test',
        emailVerified: true,
        name: 'Owner User',
        pictureUrl: 'https://example.test/owner.png',
        provider: 'Google'
      },
      complex: request.complex,
      firstCourt: request.firstCourt
    });
  });

  it('delegates the create endpoint to the use case', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue(created)
    } as unknown as CreateComplexWithFirstCourtUseCase;
    const controller = new ComplexesController(useCase);

    await expect(controller.create(authenticatedUser, request)).resolves.toEqual(created);
    expect(useCase.execute).toHaveBeenCalledWith(authenticatedUser, request);
  });

  it('creates the complex and first court inside one transaction', async () => {
    process.env.DEMO_OWNER_EMAILS = 'owner@example.test';
    delete process.env.DEMO_OWNER_SUBS;
    const transactionClient = {
      user: {
        upsert: jest.fn().mockResolvedValue({ id: 'owner-id' })
      },
      userRole: {
        findUnique: jest.fn().mockResolvedValue({ id: 'role-id' }),
        upsert: jest.fn().mockResolvedValue({ id: 'role-id' }),
        deleteMany: jest.fn()
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
    };
    const prisma = {
      $transaction: jest.fn().mockImplementation(async (callback) => callback(transactionClient))
    };
    const repository = new PrismaComplexRepository(prisma as never);

    await expect(
      repository.createComplexWithFirstCourt({
        ownerIdentity: {
          sub: 'owner-sub',
          email: 'owner@example.test',
          emailVerified: true,
          name: 'Owner User',
          pictureUrl: 'https://example.test/owner.png',
          provider: 'Google'
        },
        complex: request.complex,
        firstCourt: request.firstCourt
      })
    ).resolves.toEqual(created);

    expect(prisma.$transaction).toHaveBeenCalledTimes(1);
    expect(transactionClient.user.upsert).toHaveBeenCalledWith({
      where: { cognitoSub: 'owner-sub' },
      create: {
        cognitoSub: 'owner-sub',
        email: 'owner@example.test',
        name: 'Owner User',
        pictureUrl: 'https://example.test/owner.png',
        provider: 'Google'
      },
      update: {
        email: 'owner@example.test',
        name: 'Owner User',
        pictureUrl: 'https://example.test/owner.png',
        provider: 'Google'
      },
      select: { id: true }
    });
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
    expect(transactionClient.userRole.findUnique).toHaveBeenCalledWith({
      where: {
        userId_role: {
          userId: 'owner-id',
          role: 'OWNER'
        }
      },
      select: { id: true }
    });
    expect(transactionClient.userRole.deleteMany).not.toHaveBeenCalled();
    expect(transactionClient.complex.create).toHaveBeenCalledWith({
      data: {
        ownerId: 'owner-id',
        name: 'North Sports Center',
        address: '123 Main Street'
      }
    });
    expect(transactionClient.court.create).toHaveBeenCalledWith({
      data: {
        complexId: 'complex-id',
        name: 'Court A'
      }
    });
  });

  it('rejects creation when the authenticated user has no OWNER role', async () => {
    const transactionClient = {
      user: {
        upsert: jest.fn().mockResolvedValue({ id: 'owner-id' })
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
    };
    const prisma = {
      $transaction: jest.fn().mockImplementation(async (callback) => callback(transactionClient))
    };
    const repository = new PrismaComplexRepository(prisma as never);

    await expect(
      repository.createComplexWithFirstCourt({
        ownerIdentity: {
          sub: 'owner-sub',
          email: 'owner@example.test'
        },
        complex: request.complex,
        firstCourt: request.firstCourt
      })
    ).rejects.toBeInstanceOf(OwnerRoleRequiredError);

    expect(transactionClient.user.upsert).toHaveBeenCalledWith({
      where: { cognitoSub: 'owner-sub' },
      create: {
        cognitoSub: 'owner-sub',
        email: 'owner@example.test',
        name: undefined,
        pictureUrl: undefined,
        provider: undefined
      },
      update: {
        email: 'owner@example.test',
        name: undefined,
        pictureUrl: undefined,
        provider: undefined
      },
      select: { id: true }
    });
    expect(transactionClient.userRole.findUnique).toHaveBeenCalledWith({
      where: {
        userId_role: {
          userId: 'owner-id',
          role: 'OWNER'
        }
      },
      select: { id: true }
    });
    expect(transactionClient.userRole.upsert).not.toHaveBeenCalled();
    expect(transactionClient.userRole.deleteMany).not.toHaveBeenCalled();
  });

  it('allows a persisted OWNER to create even when not allowlisted', async () => {
    const transactionClient = {
      user: {
        upsert: jest.fn().mockResolvedValue({ id: 'owner-id' })
      },
      userRole: {
        findUnique: jest.fn().mockResolvedValue({ id: 'existing-role-id' }),
        upsert: jest.fn(),
        deleteMany: jest.fn()
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
    };
    const prisma = {
      $transaction: jest.fn().mockImplementation(async (callback) => callback(transactionClient))
    };
    const repository = new PrismaComplexRepository(prisma as never);

    await expect(
      repository.createComplexWithFirstCourt({
        ownerIdentity: {
          sub: 'owner-sub',
          email: 'owner@example.test'
        },
        complex: request.complex,
        firstCourt: request.firstCourt
      })
    ).resolves.toEqual(created);

    expect(transactionClient.userRole.upsert).not.toHaveBeenCalled();
    expect(transactionClient.userRole.deleteMany).not.toHaveBeenCalled();
  });

  it('allows an allowlisted demo owner by Cognito sub to create a complex without calling users me first', async () => {
    delete process.env.DEMO_OWNER_EMAILS;
    process.env.DEMO_OWNER_SUBS = 'owner-sub';
    const transactionClient = {
      user: {
        upsert: jest.fn().mockResolvedValue({ id: 'owner-id' })
      },
      userRole: {
        findUnique: jest.fn().mockResolvedValue({ id: 'role-id' }),
        upsert: jest.fn().mockResolvedValue({ id: 'role-id' }),
        deleteMany: jest.fn()
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
    };
    const prisma = {
      $transaction: jest.fn().mockImplementation(async (callback) => callback(transactionClient))
    };
    const repository = new PrismaComplexRepository(prisma as never);

    await expect(
      repository.createComplexWithFirstCourt({
        ownerIdentity: {
          sub: 'owner-sub',
          email: 'owner@example.test',
          name: 'Owner User',
          pictureUrl: 'https://example.test/owner.png',
          provider: 'Google'
        },
        complex: request.complex,
        firstCourt: request.firstCourt
      })
    ).resolves.toEqual(created);

    expect(transactionClient.userRole.upsert).toHaveBeenCalledTimes(1);
    expect(transactionClient.userRole.deleteMany).not.toHaveBeenCalled();
  });
});
