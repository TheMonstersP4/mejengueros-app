import { CreateComplexWithFirstCourtUseCase } from '@/modules/complexes/application/use-cases/create-complex-with-first-court.use-case';
import { InvalidComplexLocationError } from '@/modules/complexes/domain/errors/invalid-complex-location.error';
import { InvalidServiceCatalogSelectionError } from '@/modules/complexes/domain/errors/invalid-service-catalog-selection.error';
import type {
  IComplexRepository,
  ICreateComplexWithFirstCourtResult
} from '@/modules/complexes/domain/repositories/complex.repository';
import { PrismaComplexRepository } from '@/modules/complexes/infrastructure/persistence/prisma-complex.repository';
import { ComplexesController } from '@/modules/complexes/interfaces/http/controllers/complexes.controller';

describe('complexes module behavior', () => {
  function createUniqueConstraintError(): Error & { code: 'P2002' } {
    return Object.assign(new Error('Unique constraint failed'), {
      code: 'P2002' as const
    });
  }

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
      provinceId: 'province-id',
      cantonId: 'canton-id',
      address: '123 Main Street',
      latitude: 9.935,
      longitude: -84.091,
      serviceIds: ['complex-service-id']
    },
    firstCourt: {
      name: 'Court A',
      serviceIds: ['court-service-id', 'grass-service-id']
    }
  };

  const created = {
    complex: {
      id: 'complex-id',
      name: 'North Sports Center',
      provinceId: 'province-id',
      cantonId: 'canton-id',
      address: '123 Main Street',
      latitude: 9.935,
      longitude: -84.091,
      serviceIds: ['complex-service-id'],
      status: 'ACTIVE',
      createdAt: '2026-06-20T00:00:00.000Z',
      updatedAt: '2026-06-20T00:00:00.000Z'
    },
    firstCourt: {
      id: 'court-id',
      complexId: 'complex-id',
      name: 'Court A',
      serviceIds: ['court-service-id', 'grass-service-id'],
      status: 'ACTIVE',
      createdAt: '2026-06-20T00:00:00.000Z',
      updatedAt: '2026-06-20T00:00:00.000Z'
    }
  } satisfies ICreateComplexWithFirstCourtResult;

  interface IRepositoryHarnessOptions {
    provinceExists?: boolean;
    cantonMatchesProvince?: boolean;
    complexServicesFound?: boolean;
    courtServicesFound?: boolean;
    failComplexCreate?: boolean;
    failFirstUserCreateWithUniqueConstraint?: boolean;
  }

  function createRepositoryHarness(options?: IRepositoryHarnessOptions) {
    const state = {
      users: [] as Array<Record<string, unknown>>,
      userIdentities: [] as Array<Record<string, unknown>>,
      ownerRoles: [] as Array<Record<string, unknown>>,
      complexes: [] as Array<Record<string, unknown>>,
      courts: [] as Array<Record<string, unknown>>,
      complexServices: [] as Array<Record<string, string>>,
      courtServices: [] as Array<Record<string, string>>
    };

    const transactionClients: Array<Record<string, unknown>> = [];
    let injectedConcurrentIdentity = false;

    const createDraft = () => ({
      users: state.users.map((user) => ({ ...user })),
      userIdentities: state.userIdentities.map((identity) => ({ ...identity })),
      ownerRoles: state.ownerRoles.map((role) => ({ ...role })),
      complexes: state.complexes.map((complex) => ({ ...complex })),
      courts: state.courts.map((court) => ({ ...court })),
      complexServices: state.complexServices.map((service) => ({ ...service })),
      courtServices: state.courtServices.map((service) => ({ ...service }))
    });

    const commitDraft = (draft: ReturnType<typeof createDraft>) => {
      state.users = draft.users;
      state.userIdentities = draft.userIdentities;
      state.ownerRoles = draft.ownerRoles;
      state.complexes = draft.complexes;
      state.courts = draft.courts;
      state.complexServices = draft.complexServices;
      state.courtServices = draft.courtServices;
    };

    const injectConcurrentIdentityWinner = () => {
      state.users = [
        {
          id: 'owner-id',
          email: 'owner@example.test',
          name: 'Owner User',
          pictureUrl: 'https://example.test/owner.png'
        }
      ];
      state.userIdentities = [
        {
          userId: 'owner-id',
          provider: 'Google',
          providerSubject: 'owner-sub',
          emailAtLogin: 'owner@example.test'
        }
      ];
    };

    const createTransactionClient = (draft: ReturnType<typeof createDraft>) => ({
      province: {
        findUnique: jest.fn().mockResolvedValue(
          options?.provinceExists === false ? null : { id: 'province-id' }
        )
      },
      canton: {
        findFirst: jest.fn().mockResolvedValue(
          options?.cantonMatchesProvince === false ? null : { id: 'canton-id' }
        )
      },
      serviceCatalog: {
        findMany: jest
          .fn()
          .mockImplementation(async ({ where }: { where: { scope: string } }) => {
            if (where.scope === 'COMPLEX') {
              return options?.complexServicesFound === false
                ? []
                : [{ id: 'complex-service-id' }];
            }

            return options?.courtServicesFound === false
              ? [{ id: 'court-service-id' }]
              : [{ id: 'court-service-id' }, { id: 'grass-service-id' }];
          })
      },
      user: {
        findUnique: jest.fn().mockImplementation(async ({ where }: { where: { email: string } }) => {
          const user = draft.users.find((entry) => entry.email === where.email);

          return user
            ? {
                ...user,
                identities: draft.userIdentities.filter((identity) => identity.userId === user.id)
              }
            : null;
        }),
        update: jest.fn().mockImplementation(async ({ where, data }: { where: { id: string }; data: Record<string, unknown> }) => {
          const user = draft.users.find((entry) => entry.id === where.id);

          if (!user) {
            throw new Error(`user-not-found:${where.id}`);
          }

          Object.assign(user, data);

          if ('identities' in data && data.identities && typeof data.identities === 'object') {
            const identityCreate = (data.identities as { create?: Record<string, unknown> }).create;

            if (identityCreate) {
              draft.userIdentities.push({ userId: where.id, ...identityCreate });
            }
          }

          return { id: where.id };
        }),
        create: jest.fn().mockImplementation(async ({ data }: { data: Record<string, unknown> }) => {
          if (
            options?.failFirstUserCreateWithUniqueConstraint === true &&
            injectedConcurrentIdentity === false
          ) {
            injectedConcurrentIdentity = true;
            injectConcurrentIdentityWinner();
            throw createUniqueConstraintError();
          }

          draft.users.push({
            id: 'owner-id',
            email: data.email,
            name: data.name,
            pictureUrl: data.pictureUrl
          });

          const identityCreate = (data.identities as { create: Record<string, unknown> }).create;
          draft.userIdentities.push({ userId: 'owner-id', ...identityCreate });

          return { id: 'owner-id' };
        })
      },
      userIdentity: {
        findUnique: jest
          .fn()
          .mockImplementation(async ({ where }: { where: { provider_providerSubject: { provider: string; providerSubject: string } } }) => {
            const identity = draft.userIdentities.find(
              (entry) =>
                entry.provider === where.provider_providerSubject.provider &&
                entry.providerSubject === where.provider_providerSubject.providerSubject
            );

            if (!identity) {
              return null;
            }

            const user = draft.users.find((entry) => entry.id === identity.userId);

            return user ? { userId: String(identity.userId), user: { ...user, identities: [] } } : null;
          })
      },
      userRole: {
        upsert: jest.fn().mockImplementation(async ({ create }: { create: { userId: string; role: 'OWNER' } }) => {
          if (
            !draft.ownerRoles.some(
              (entry) => entry.userId === create.userId && entry.role === create.role
            )
          ) {
            draft.ownerRoles.push({ userId: create.userId, role: create.role });
          }

          return { id: 'owner-role-id' };
        })
      },
      complex: {
        create: jest.fn().mockImplementation(async ({ data }: { data: Record<string, unknown> }) => {
          if (options?.failComplexCreate === true) {
            throw new Error('complex-create-failed');
          }

          draft.complexes.push(data);

          return {
            id: 'complex-id',
            name: 'North Sports Center',
            provinceId: 'province-id',
            cantonId: 'canton-id',
            address: '123 Main Street',
            latitude: 9.935,
            longitude: -84.091,
            status: 'ACTIVE',
            createdAt: new Date('2026-06-20T00:00:00.000Z'),
            updatedAt: new Date('2026-06-20T00:00:00.000Z')
          };
        })
      },
      court: {
        create: jest.fn().mockImplementation(async ({ data }: { data: Record<string, unknown> }) => {
          draft.courts.push(data);

          return {
            id: 'court-id',
            complexId: 'complex-id',
            name: 'Court A',
            status: 'ACTIVE',
            createdAt: new Date('2026-06-20T00:00:00.000Z'),
            updatedAt: new Date('2026-06-20T00:00:00.000Z')
          };
        })
      },
      complexService: {
        createMany: jest.fn().mockImplementation(async ({ data }: { data: Array<Record<string, string>> }) => {
          draft.complexServices.push(...data);
          return { count: data.length };
        })
      },
      courtService: {
        createMany: jest.fn().mockImplementation(async ({ data }: { data: Array<Record<string, string>> }) => {
          draft.courtServices.push(...data);
          return { count: data.length };
        })
      }
    });

    type ITransactionClientMock = ReturnType<typeof createTransactionClient>;

    const prisma = {
      $transaction: jest.fn().mockImplementation(async (callback) => {
        const draft = createDraft();
        const transactionClient = createTransactionClient(draft);
        transactionClients.push(transactionClient);

        const result = await callback(transactionClient as never);
        commitDraft(draft);
        return result;
      })
    };

    return { prisma, state, transactionClients: transactionClients as ITransactionClientMock[] };
  }

  it('forwards the expanded wizard payload through the repository port', async () => {
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

  it('creates the complex, first court, owner role, and service associations atomically', async () => {
    const harness = createRepositoryHarness();
    const repository = new PrismaComplexRepository(harness.prisma as never);

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

    expect(harness.prisma.$transaction).toHaveBeenCalledTimes(1);
    const transactionClient = harness.transactionClients[0];
    expect(transactionClient.province.findUnique).toHaveBeenCalledWith({
      where: { id: 'province-id' },
      select: { id: true }
    });
    expect(transactionClient.canton.findFirst).toHaveBeenCalledWith({
      where: { id: 'canton-id', provinceId: 'province-id' },
      select: { id: true }
    });
    expect(transactionClient.user.create).toHaveBeenCalledTimes(1);
    expect(transactionClient.userRole.upsert).toHaveBeenCalledTimes(1);
    expect(transactionClient.complex.create).toHaveBeenCalledWith({
      data: {
        ownerId: 'owner-id',
        name: 'North Sports Center',
        provinceId: 'province-id',
        cantonId: 'canton-id',
        address: '123 Main Street',
        latitude: 9.935,
        longitude: -84.091
      }
    });
    expect(transactionClient.complexService.createMany).toHaveBeenCalledWith({
      data: [{ complexId: 'complex-id', serviceCatalogId: 'complex-service-id' }]
    });
    expect(transactionClient.courtService.createMany).toHaveBeenCalledWith({
      data: [
        { courtId: 'court-id', serviceCatalogId: 'court-service-id' },
        { courtId: 'court-id', serviceCatalogId: 'grass-service-id' }
      ]
    });
    expect(harness.state.userIdentities).toEqual([
      {
        userId: 'owner-id',
        provider: 'Google',
        providerSubject: 'owner-sub',
        emailAtLogin: 'owner@example.test'
      }
    ]);
    expect(harness.state.ownerRoles).toEqual([{ userId: 'owner-id', role: 'OWNER' }]);
    expect(harness.state.complexServices).toEqual([
      { complexId: 'complex-id', serviceCatalogId: 'complex-service-id' }
    ]);
    expect(harness.state.courtServices).toEqual([
      { courtId: 'court-id', serviceCatalogId: 'court-service-id' },
      { courtId: 'court-id', serviceCatalogId: 'grass-service-id' }
    ]);
  });

  it('rejects an unknown province without creating identity or owner role side effects', async () => {
    const harness = createRepositoryHarness({ provinceExists: false });
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
    ).rejects.toBeInstanceOf(InvalidComplexLocationError);

    const transactionClient = harness.transactionClients[0];
    expect(transactionClient.userIdentity.findUnique).not.toHaveBeenCalled();
    expect(transactionClient.user.create).not.toHaveBeenCalled();
    expect(transactionClient.userRole.upsert).not.toHaveBeenCalled();
    expect(harness.state.users).toEqual([]);
    expect(harness.state.userIdentities).toEqual([]);
    expect(harness.state.ownerRoles).toEqual([]);
  });

  it('rejects a canton that does not belong to the selected province', async () => {
    const harness = createRepositoryHarness({ cantonMatchesProvince: false });
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
    ).rejects.toBeInstanceOf(InvalidComplexLocationError);

    const transactionClient = harness.transactionClients[0];
    expect(transactionClient.userIdentity.findUnique).not.toHaveBeenCalled();
    expect(transactionClient.user.create).not.toHaveBeenCalled();
    expect(transactionClient.userRole.upsert).not.toHaveBeenCalled();
    expect(harness.state.users).toEqual([]);
    expect(harness.state.userIdentities).toEqual([]);
    expect(harness.state.ownerRoles).toEqual([]);
    expect(harness.state.complexes).toEqual([]);
    expect(harness.state.courts).toEqual([]);
  });

  it('rejects service ids outside the expected scope or inactive services', async () => {
    const harness = createRepositoryHarness({ courtServicesFound: false });
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
    ).rejects.toBeInstanceOf(InvalidServiceCatalogSelectionError);

    const transactionClient = harness.transactionClients[0];
    expect(transactionClient.userIdentity.findUnique).not.toHaveBeenCalled();
    expect(transactionClient.user.create).not.toHaveBeenCalled();
    expect(transactionClient.userRole.upsert).not.toHaveBeenCalled();
    expect(transactionClient.complex.create).not.toHaveBeenCalled();
    expect(harness.state.users).toEqual([]);
    expect(harness.state.userIdentities).toEqual([]);
    expect(harness.state.ownerRoles).toEqual([]);
  });

  it('rolls back owner role and write state when complex creation fails after validation', async () => {
    const harness = createRepositoryHarness({ failComplexCreate: true });
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
    const transactionClient = harness.transactionClients[0];
    expect(transactionClient.user.create).toHaveBeenCalledTimes(1);
    expect(transactionClient.userRole.upsert).toHaveBeenCalledTimes(1);
    expect(harness.state.users).toEqual([]);
    expect(harness.state.userIdentities).toEqual([]);
    expect(harness.state.ownerRoles).toEqual([]);
    expect(harness.state.complexes).toEqual([]);
    expect(harness.state.courts).toEqual([]);
    expect(harness.state.complexServices).toEqual([]);
    expect(harness.state.courtServices).toEqual([]);
  });

  it('retries the whole transaction when identity creation loses a P2002 race', async () => {
    const harness = createRepositoryHarness({ failFirstUserCreateWithUniqueConstraint: true });
    const repository = new PrismaComplexRepository(harness.prisma as never);

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

    expect(harness.prisma.$transaction).toHaveBeenCalledTimes(2);

    const firstTransactionClient = harness.transactionClients[0];
    const secondTransactionClient = harness.transactionClients[1];

    expect(firstTransactionClient.user.create).toHaveBeenCalledTimes(1);
    expect(firstTransactionClient.userRole.upsert).not.toHaveBeenCalled();
    expect(secondTransactionClient.userIdentity.findUnique).toHaveBeenCalledTimes(1);
    expect(secondTransactionClient.user.update).toHaveBeenCalledWith({
      where: { id: 'owner-id' },
      data: {
        email: 'owner@example.test',
        name: 'Owner User',
        pictureUrl: 'https://example.test/owner.png'
      },
      select: { id: true }
    });
    expect(harness.state.userIdentities).toEqual([
      {
        userId: 'owner-id',
        provider: 'Google',
        providerSubject: 'owner-sub',
        emailAtLogin: 'owner@example.test'
      }
    ]);
    expect(harness.state.ownerRoles).toEqual([{ userId: 'owner-id', role: 'OWNER' }]);
  });
});
