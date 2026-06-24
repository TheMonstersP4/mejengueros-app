import { CreateComplexWithFirstCourtUseCase } from '@/modules/complexes/application/use-cases/create-complex-with-first-court.use-case';
import type {
  IComplexRepository,
  ICreateComplexWithFirstCourtResult
} from '@/modules/complexes/domain/repositories/complex.repository';
import { PrismaComplexRepository } from '@/modules/complexes/infrastructure/persistence/prisma-complex.repository';
import { ComplexesController } from '@/modules/complexes/interfaces/http/controllers/complexes.controller';

describe('complexes module behavior', () => {
  interface ITransactionalState {
    userIds: string[];
    ownerRoles: Array<{ userId: string; role: 'OWNER' }>;
    complexes: Array<{ id: string; ownerId: string; name: string; address: string }>;
    courts: Array<{ id: string; complexId: string; name: string }>;
  }

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

  function createTransactionalPrismaHarness(options?: {
    failComplexCreate?: boolean;
  }): {
    state: ITransactionalState;
    prisma: {
      user: {
        create: jest.Mock;
        findUnique: jest.Mock;
        update: jest.Mock;
      };
      userIdentity: { findUnique: jest.Mock };
      userRole: { upsert: jest.Mock };
      complex: { create: jest.Mock };
      court: { create: jest.Mock };
      $transaction: jest.Mock;
    };
  } {
    const state: ITransactionalState = {
      userIds: [],
      ownerRoles: [],
      complexes: [],
      courts: []
    };

    const cloneState = (current: ITransactionalState): ITransactionalState => ({
      userIds: [...current.userIds],
      ownerRoles: current.ownerRoles.map((role) => ({ ...role })),
      complexes: current.complexes.map((complex) => ({ ...complex })),
      courts: current.courts.map((court) => ({ ...court }))
    });

    const prisma = {
      user: {
        create: jest.fn(),
        findUnique: jest.fn(),
        update: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn()
      },
      userRole: {
        upsert: jest.fn()
      },
      complex: {
        create: jest.fn()
      },
      court: {
        create: jest.fn()
      },
      $transaction: jest.fn(async (callback: (client: {
        user: {
          create: jest.Mock;
          findUnique: jest.Mock;
          update: jest.Mock;
        };
        userIdentity: { findUnique: jest.Mock };
        userRole: { upsert: jest.Mock; deleteMany: jest.Mock };
        complex: { create: jest.Mock };
        court: { create: jest.Mock };
      }) => Promise<unknown>) => {
        const transactionState = cloneState(state);
        const transactionClient = {
          user: {
            create: jest.fn().mockImplementation(async () => {
              transactionState.userIds.push('owner-id');

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
              transactionState.ownerRoles.push({ userId: 'owner-id', role: 'OWNER' });

              return { id: 'owner-role-id' };
            }),
            deleteMany: jest.fn()
          },
          complex: {
            create: jest.fn().mockImplementation(async () => {
              if (options?.failComplexCreate === true) {
                throw new Error('complex-create-failed');
              }

              transactionState.complexes.push({
                id: 'complex-id',
                ownerId: 'owner-id',
                name: 'North Sports Center',
                address: '123 Main Street'
              });

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
              transactionState.courts.push({
                id: 'court-id',
                complexId: 'complex-id',
                name: 'Court A'
              });

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
      })
    };

    return { state, prisma };
  }

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

  it('creates the complex, first court, and OWNER role inside one transaction for an authenticated user without OWNER', async () => {
    const transactionClient = {
      user: {
        create: jest.fn().mockResolvedValue({ id: 'owner-id' }),
        findUnique: jest.fn().mockResolvedValue(null),
        update: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
      },
      userRole: {
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
    expect(transactionClient.user.create).toHaveBeenCalledWith({
      data: {
        email: 'owner@example.test',
        name: 'Owner User',
        pictureUrl: 'https://example.test/owner.png',
        identities: {
          create: {
            provider: 'Google',
            providerSubject: 'owner-sub',
            emailAtLogin: 'owner@example.test'
          }
        }
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

  it('preserves an existing PLAYER role while adding OWNER for the first complex', async () => {
    const transactionClient = {
      user: {
        create: jest.fn().mockResolvedValue({ id: 'owner-id' }),
        findUnique: jest.fn().mockResolvedValue(null),
        update: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
      },
      userRole: {
        upsert: jest.fn().mockResolvedValue({ id: 'owner-role-id' }),
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
          provider: 'Google'
        },
        complex: request.complex,
        firstCourt: request.firstCourt
      })
    ).resolves.toEqual(created);

    expect(transactionClient.user.create).toHaveBeenCalledWith({
      data: {
        email: 'owner@example.test',
        identities: {
          create: {
            provider: 'Google',
            providerSubject: 'owner-sub',
            emailAtLogin: 'owner@example.test'
          }
        }
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
    expect(transactionClient.userRole.deleteMany).not.toHaveBeenCalled();
    expect(transactionClient.complex.create).toHaveBeenCalledTimes(1);
    expect(transactionClient.court.create).toHaveBeenCalledTimes(1);
  });

  it('allows a persisted OWNER to create without duplicating the OWNER assignment', async () => {
    const transactionClient = {
      user: {
        create: jest.fn().mockResolvedValue({ id: 'owner-id' }),
        findUnique: jest.fn().mockResolvedValue(null),
        update: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
      },
      userRole: {
        upsert: jest.fn().mockResolvedValue({ id: 'existing-role-id' }),
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

    expect(transactionClient.userRole.upsert).toHaveBeenCalledTimes(1);
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
    expect(transactionClient.userRole.deleteMany).not.toHaveBeenCalled();
  });

  it('allows an allowlisted demo owner by Cognito sub to create a complex without calling users me first', async () => {
    delete process.env.DEMO_OWNER_EMAILS;
    process.env.DEMO_OWNER_SUBS = 'owner-sub';
    const transactionClient = {
      user: {
        create: jest.fn().mockResolvedValue({ id: 'owner-id' }),
        findUnique: jest.fn().mockResolvedValue(null),
        update: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
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

  it('fails the whole transaction when complex creation errors after OWNER upsert and leaves no persisted OWNER role, complex, or court', async () => {
    const harness = createTransactionalPrismaHarness({ failComplexCreate: true });
    const repository = new PrismaComplexRepository(harness.prisma as never);

    await expect(
      repository.createComplexWithFirstCourt({
        ownerIdentity: {
          sub: 'owner-sub',
          email: 'owner@example.test'
        },
        complex: request.complex,
        firstCourt: request.firstCourt
      })
    ).rejects.toThrow('complex-create-failed');

    expect(harness.prisma.$transaction).toHaveBeenCalledTimes(1);
    expect(harness.state.ownerRoles).toEqual([]);
    expect(harness.state.complexes).toEqual([]);
    expect(harness.state.courts).toEqual([]);
    expect(harness.prisma.userRole.upsert).not.toHaveBeenCalled();
    expect(harness.prisma.complex.create).not.toHaveBeenCalled();
    expect(harness.prisma.court.create).not.toHaveBeenCalled();
  });
});
