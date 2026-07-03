import { CreateComplexWithFirstCourtUseCase } from '@/modules/complexes/application/use-cases/create-complex-with-first-court.use-case';
import { CreateCourtForOwnedComplexUseCase } from '@/modules/complexes/application/use-cases/create-court-for-owned-complex.use-case';
import { UpdateOwnedCourtImageUseCase } from '@/modules/complexes/application/use-cases/update-owned-court-image.use-case';
import { ComplexNotFoundForOwnerError } from '@/modules/complexes/domain/errors/complex-not-found-for-owner.error';
import { CourtNotFoundForOwnerError } from '@/modules/complexes/domain/errors/court-not-found-for-owner.error';
import { GetMyComplexHubUseCase } from '@/modules/complexes/application/use-cases/get-my-complex-hub.use-case';
import { InvalidCourtImageUploadError } from '@/modules/complexes/domain/errors/invalid-court-image-upload.error';
import { InvalidComplexLocationError } from '@/modules/complexes/domain/errors/invalid-complex-location.error';
import { InvalidServiceCatalogSelectionError } from '@/modules/complexes/domain/errors/invalid-service-catalog-selection.error';
import { ImageUploadEntity } from '@/modules/files/domain/entities/image-upload.entity';
import { FilePurpose } from '@/modules/files/domain/enums/file-purpose.enum';
import type { IImageUploadRepository } from '@/modules/files/domain/repositories/image-upload.repository';
import type {
  IComplexRepository,
  ICreateComplexWithFirstCourtResult,
  IGetMyComplexHubResult
} from '@/modules/complexes/domain/repositories/complex.repository';
import type { IFileReadUrlPort } from '@/modules/files/application/ports/file-read-url.port';
import { PrismaComplexRepository } from '@/modules/complexes/infrastructure/persistence/prisma-complex.repository';
import { ComplexesController } from '@/modules/complexes/interfaces/http/controllers/complexes.controller';

describe('complexes module behavior', () => {
  function createUniqueConstraintError(target?: string | string[]): Error & {
    code: 'P2002';
    meta?: { target?: string | string[] };
  } {
    return Object.assign(new Error('Unique constraint failed'), {
      code: 'P2002' as const,
      meta: target == null ? undefined : { target }
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

  const requestWithCourtImage = {
    ...request,
    firstCourt: {
      ...request.firstCourt,
      imageUploadId: 'court-image-id'
    }
  };

  const updateCourtImageRequest = {
    imageUploadId: 'court-image-id'
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

  const myHub = {
    complexes: [
      {
        id: 'complex-id',
        name: 'North Sports Center',
        address: '123 Main Street',
        provinceId: 'province-id',
        cantonId: 'canton-id',
        latitude: 9.935,
        longitude: -84.091,
        status: 'ACTIVE',
        courts: [
          {
            id: 'court-configured-id',
            name: 'Court A',
            status: 'ACTIVE',
            availabilityStatus: 'CONFIGURED',
            imageUrl: null
          },
          {
            id: 'court-pending-id',
            name: 'Court B',
            status: 'ACTIVE',
            availabilityStatus: 'PENDING',
            imageUrl: null
          }
        ]
      }
    ]
  } satisfies IGetMyComplexHubResult;

  interface IRepositoryHarnessOptions {
    provinceExists?: boolean;
    cantonMatchesProvince?: boolean;
    ownedComplexExists?: boolean;
    ownedCourtExists?: boolean;
    complexServicesFound?: boolean;
    courtServicesFound?: boolean;
    failComplexCreate?: boolean;
    failCourtCreateWithImageUploadUniqueConstraint?: boolean;
    failCourtServiceCreate?: boolean;
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
        findFirst: jest.fn().mockImplementation(async ({ where }: { where: { id: string } }) => {
          if (options?.ownedComplexExists === false || where.id !== 'complex-id') {
            return null;
          }

          return { id: 'complex-id' };
        }),
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
        findFirst: jest.fn().mockImplementation(async ({ where }: { where: { id?: string; complexId?: string } }) => {
          if (where.id === 'court-id') {
            return options?.ownedCourtExists === false ? null : { id: 'court-id' };
          }

          return null;
        }),
        create: jest.fn().mockImplementation(async ({ data }: { data: Record<string, unknown> }) => {
          if (options?.failCourtCreateWithImageUploadUniqueConstraint === true) {
            throw createUniqueConstraintError(['imageUploadId']);
          }

          draft.courts.push(data);

          return {
            id: 'court-id',
            complexId: 'complex-id',
            name: 'Court A',
            status: 'ACTIVE',
            createdAt: new Date('2026-06-20T00:00:00.000Z'),
            updatedAt: new Date('2026-06-20T00:00:00.000Z')
          };
        }),
        update: jest.fn().mockImplementation(async () => {
          if (options?.failCourtCreateWithImageUploadUniqueConstraint === true) {
            throw createUniqueConstraintError(['imageUploadId']);
          }

          return {
            id: 'court-id',
            name: 'Court A',
            status: 'ACTIVE',
            availability: { id: 'availability-id' },
            imageUpload: {
              objectKey: 'dev/uploads/court-image/owner-sub/2026/06/court-image.png'
            }
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
          if (options?.failCourtServiceCreate === true) {
            throw new Error('court-service-create-failed');
          }

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
      }),
      court: {
        findFirst: jest.fn().mockResolvedValue(null)
      }
    };

    return { prisma, state, transactionClients: transactionClients as ITransactionClientMock[] };
  }

  it('forwards the expanded wizard payload through the repository port', async () => {
    const repository = createComplexRepositoryDouble({
      createComplexWithFirstCourt: jest.fn().mockResolvedValue(created)
    });
    const imageUploadRepository = createImageUploadRepository();
    const useCase = new CreateComplexWithFirstCourtUseCase(repository, imageUploadRepository);

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

  it('forwards an optional validated court image upload id through the repository port', async () => {
    const repository = createComplexRepositoryDouble({
      createComplexWithFirstCourt: jest.fn().mockResolvedValue(created)
    });
    const imageUploadRepository = createImageUploadRepository({
      imageUpload:
        ImageUploadEntity.fromPersistence({
          id: 'court-image-id',
          ownerSub: 'owner-sub',
          ownerEmail: 'owner@example.test',
          ownerName: 'Owner User',
          ownerPictureUrl: 'https://example.test/owner.png',
          ownerProvider: 'Google',
          purpose: FilePurpose.CourtImage,
          objectKey: 'dev/uploads/court-image/owner-sub/2026/06/court-image.png',
          contentType: 'image/png',
          sizeBytes: 512,
          createdAt: new Date('2026-06-20T00:00:00.000Z')
        })
    });
    const useCase = new CreateComplexWithFirstCourtUseCase(repository, imageUploadRepository);

    await expect(useCase.execute(authenticatedUser, requestWithCourtImage)).resolves.toEqual(created);
    expect(imageUploadRepository.findById).toHaveBeenCalledWith('court-image-id');
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
      firstCourt: requestWithCourtImage.firstCourt
    });
  });

  it('rejects a first-court image upload owned by another user', async () => {
    const imageUploadRepository = createImageUploadRepository({
      imageUpload:
        ImageUploadEntity.fromPersistence({
          id: 'court-image-id',
          ownerSub: 'another-owner',
          ownerEmail: 'owner@example.test',
          ownerName: 'Owner User',
          ownerPictureUrl: 'https://example.test/owner.png',
          ownerProvider: 'Google',
          purpose: FilePurpose.CourtImage,
          objectKey: 'dev/uploads/court-image/another-owner/2026/06/court-image.png',
          contentType: 'image/png',
          sizeBytes: 512,
          createdAt: new Date('2026-06-20T00:00:00.000Z')
        })
    });
    const useCase = new CreateComplexWithFirstCourtUseCase(
      createComplexRepositoryDouble(),
      imageUploadRepository
    );

    await expect(useCase.execute(authenticatedUser, requestWithCourtImage)).rejects.toBeInstanceOf(
      InvalidCourtImageUploadError
    );
  });

  it('rejects a first-court image upload with a non-court purpose', async () => {
    const imageUploadRepository = createImageUploadRepository({
      imageUpload:
        ImageUploadEntity.fromPersistence({
          id: 'court-image-id',
          ownerSub: 'owner-sub',
          ownerEmail: 'owner@example.test',
          ownerName: 'Owner User',
          ownerPictureUrl: 'https://example.test/owner.png',
          ownerProvider: 'Google',
          purpose: FilePurpose.ProfileImage,
          objectKey: 'dev/uploads/profile-image/owner-sub/2026/06/profile-image.png',
          contentType: 'image/png',
          sizeBytes: 512,
          createdAt: new Date('2026-06-20T00:00:00.000Z')
        })
    });
    const useCase = new CreateComplexWithFirstCourtUseCase(
      createComplexRepositoryDouble(),
      imageUploadRepository
    );

    await expect(useCase.execute(authenticatedUser, requestWithCourtImage)).rejects.toBeInstanceOf(
      InvalidCourtImageUploadError
    );
  });

  it('delegates the create endpoint to the use case', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue(created)
    } as unknown as CreateComplexWithFirstCourtUseCase;
    const controller = new ComplexesController(
      useCase,
      {} as CreateCourtForOwnedComplexUseCase,
      {} as UpdateOwnedCourtImageUseCase,
      {} as GetMyComplexHubUseCase
    );

    await expect(controller.create(authenticatedUser, request)).resolves.toEqual(created);
    expect(useCase.execute).toHaveBeenCalledWith(authenticatedUser, request);
  });

  it('loads the authenticated owner hub through the repository port', async () => {
    const repository = createComplexRepositoryDouble({
      getMyComplexHub: jest.fn().mockResolvedValue(myHub)
    });
    const useCase = new GetMyComplexHubUseCase(repository);

    await expect(useCase.execute(authenticatedUser)).resolves.toEqual(myHub);
    expect(repository.getMyComplexHub).toHaveBeenCalledWith({
      ownerIdentity: {
        sub: 'owner-sub',
        provider: 'Google'
      }
    });
  });

  it('forwards the add-court payload through the repository port', async () => {
    const repository = createComplexRepositoryDouble({
      createOwnedComplexCourt: jest.fn().mockResolvedValue(created.firstCourt)
    });
    const useCase = new CreateCourtForOwnedComplexUseCase(repository, createImageUploadRepository());

    await expect(useCase.execute(authenticatedUser, 'complex-id', request.firstCourt)).resolves.toEqual(
      created.firstCourt
    );
    expect(repository.createOwnedComplexCourt).toHaveBeenCalledWith({
      ownerIdentity: {
        sub: 'owner-sub',
        provider: 'Google'
      },
      complexId: 'complex-id',
      court: request.firstCourt
    });
  });

  it('forwards a validated image upload id when adding a court to an owned complex', async () => {
    const repository = createComplexRepositoryDouble({
      createOwnedComplexCourt: jest.fn().mockResolvedValue(created.firstCourt)
    });
    const imageUploadRepository = createImageUploadRepository({
      imageUpload:
        ImageUploadEntity.fromPersistence({
          id: 'court-image-id',
          ownerSub: 'owner-sub',
          ownerEmail: 'owner@example.test',
          ownerName: 'Owner User',
          ownerPictureUrl: 'https://example.test/owner.png',
          ownerProvider: 'Google',
          purpose: FilePurpose.CourtImage,
          objectKey: 'dev/uploads/court-image/owner-sub/2026/06/court-image.png',
          contentType: 'image/png',
          sizeBytes: 512,
          createdAt: new Date('2026-06-20T00:00:00.000Z')
        })
    });
    const useCase = new CreateCourtForOwnedComplexUseCase(repository, imageUploadRepository);

    await expect(
      useCase.execute(authenticatedUser, 'complex-id', requestWithCourtImage.firstCourt)
    ).resolves.toEqual(created.firstCourt);
    expect(imageUploadRepository.findById).toHaveBeenCalledWith('court-image-id');
    expect(repository.createOwnedComplexCourt).toHaveBeenCalledWith({
      ownerIdentity: {
        sub: 'owner-sub',
        provider: 'Google'
      },
      complexId: 'complex-id',
      court: requestWithCourtImage.firstCourt
    });
  });

  it('forwards the update-court-image payload through the repository port', async () => {
    const repository = createComplexRepositoryDouble({
      updateOwnedCourtImage: jest.fn().mockResolvedValue(myHub.complexes[0].courts[0])
    });
    const imageUploadRepository = createImageUploadRepository({
      imageUpload:
        ImageUploadEntity.fromPersistence({
          id: 'court-image-id',
          ownerSub: 'owner-sub',
          ownerEmail: 'owner@example.test',
          ownerName: 'Owner User',
          ownerPictureUrl: 'https://example.test/owner.png',
          ownerProvider: 'Google',
          purpose: FilePurpose.CourtImage,
          objectKey: 'dev/uploads/court-image/owner-sub/2026/06/court-image.png',
          contentType: 'image/png',
          sizeBytes: 512,
          createdAt: new Date('2026-06-20T00:00:00.000Z')
        })
    });
    const useCase = new UpdateOwnedCourtImageUseCase(repository, imageUploadRepository);

    await expect(
      useCase.execute(authenticatedUser, 'complex-id', 'court-id', updateCourtImageRequest)
    ).resolves.toEqual(myHub.complexes[0].courts[0]);
    expect(repository.updateOwnedCourtImage).toHaveBeenCalledWith({
      ownerIdentity: {
        sub: 'owner-sub',
        provider: 'Google'
      },
      complexId: 'complex-id',
      courtId: 'court-id',
      imageUploadId: 'court-image-id'
    });
  });

  it('rejects update-court-image when the confirmed upload belongs to another user', async () => {
    const imageUploadRepository = createImageUploadRepository({
      imageUpload:
        ImageUploadEntity.fromPersistence({
          id: 'court-image-id',
          ownerSub: 'another-owner',
          ownerEmail: 'owner@example.test',
          ownerName: 'Owner User',
          ownerPictureUrl: 'https://example.test/owner.png',
          ownerProvider: 'Google',
          purpose: FilePurpose.CourtImage,
          objectKey: 'dev/uploads/court-image/another-owner/2026/06/court-image.png',
          contentType: 'image/png',
          sizeBytes: 512,
          createdAt: new Date('2026-06-20T00:00:00.000Z')
        })
    });
    const useCase = new UpdateOwnedCourtImageUseCase(createComplexRepositoryDouble(), imageUploadRepository);

    await expect(
      useCase.execute(authenticatedUser, 'complex-id', 'court-id', updateCourtImageRequest)
    ).rejects.toBeInstanceOf(InvalidCourtImageUploadError);
  });

  it('rejects update-court-image when the confirmed upload purpose is not court-image', async () => {
    const imageUploadRepository = createImageUploadRepository({
      imageUpload:
        ImageUploadEntity.fromPersistence({
          id: 'court-image-id',
          ownerSub: 'owner-sub',
          ownerEmail: 'owner@example.test',
          ownerName: 'Owner User',
          ownerPictureUrl: 'https://example.test/owner.png',
          ownerProvider: 'Google',
          purpose: FilePurpose.ProfileImage,
          objectKey: 'dev/uploads/profile-image/owner-sub/2026/06/profile-image.png',
          contentType: 'image/png',
          sizeBytes: 512,
          createdAt: new Date('2026-06-20T00:00:00.000Z')
        })
    });
    const useCase = new UpdateOwnedCourtImageUseCase(createComplexRepositoryDouble(), imageUploadRepository);

    await expect(
      useCase.execute(authenticatedUser, 'complex-id', 'court-id', updateCourtImageRequest)
    ).rejects.toBeInstanceOf(InvalidCourtImageUploadError);
  });

  it('delegates the update-court-image endpoint to the use case', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue(myHub.complexes[0].courts[0])
    } as unknown as UpdateOwnedCourtImageUseCase;
    const controller = new ComplexesController(
      {} as CreateComplexWithFirstCourtUseCase,
      {} as CreateCourtForOwnedComplexUseCase,
      useCase,
      {} as GetMyComplexHubUseCase
    );

    await expect(
      controller.updateCourtImage(authenticatedUser, 'complex-id', 'court-id', updateCourtImageRequest)
    ).resolves.toEqual({
      court: myHub.complexes[0].courts[0]
    });
    expect(useCase.execute).toHaveBeenCalledWith(
      authenticatedUser,
      'complex-id',
      'court-id',
      updateCourtImageRequest
    );
  });

  it('delegates the my hub endpoint to the query use case', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue(myHub)
    } as unknown as GetMyComplexHubUseCase;
    const controller = new ComplexesController(
      {} as CreateComplexWithFirstCourtUseCase,
      {} as CreateCourtForOwnedComplexUseCase,
      {} as UpdateOwnedCourtImageUseCase,
      useCase
    );

    await expect(controller.getMyHub(authenticatedUser)).resolves.toEqual(myHub);
    expect(useCase.execute).toHaveBeenCalledWith(authenticatedUser);
  });

  it('delegates the add-court endpoint to the use case', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue(created.firstCourt)
    } as unknown as CreateCourtForOwnedComplexUseCase;
    const controller = new ComplexesController(
      {} as CreateComplexWithFirstCourtUseCase,
      useCase,
      {} as UpdateOwnedCourtImageUseCase,
      {} as GetMyComplexHubUseCase
    );

    await expect(controller.createCourt(authenticatedUser, 'complex-id', request.firstCourt)).resolves.toEqual({
      court: created.firstCourt
    });
    expect(useCase.execute).toHaveBeenCalledWith(authenticatedUser, 'complex-id', request.firstCourt);
  });

  it('creates the complex, first court, owner role, and service associations atomically', async () => {
    const harness = createRepositoryHarness();
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

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
        longitude: -84.091,
        isPublished: true
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
    expect(transactionClient.court.create).toHaveBeenCalledWith({
      data: {
        complexId: 'complex-id',
        name: 'Court A'
      }
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

  it('persists the optional first-court image upload association when present', async () => {
    const harness = createRepositoryHarness();
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

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
        firstCourt: requestWithCourtImage.firstCourt
      })
    ).resolves.toEqual(created);

    const transactionClient = harness.transactionClients[0];
    expect(transactionClient.court.create).toHaveBeenCalledWith({
      data: {
        complexId: 'complex-id',
        name: 'Court A',
        imageUploadId: 'court-image-id'
      }
    });
  });

  it('rejects a first-court image upload already assigned to another court', async () => {
    const repository = createComplexRepositoryDouble({
      findCourtIdByImageUploadId: jest.fn().mockResolvedValue('existing-court-id')
    });
    const imageUploadRepository = createImageUploadRepository({
      imageUpload:
        ImageUploadEntity.fromPersistence({
          id: 'court-image-id',
          ownerSub: 'owner-sub',
          ownerEmail: 'owner@example.test',
          ownerName: 'Owner User',
          ownerPictureUrl: 'https://example.test/owner.png',
          ownerProvider: 'Google',
          purpose: FilePurpose.CourtImage,
          objectKey: 'dev/uploads/court-image/owner-sub/2026/06/court-image.png',
          contentType: 'image/png',
          sizeBytes: 512,
          createdAt: new Date('2026-06-20T00:00:00.000Z')
        })
    });
    const useCase = new CreateComplexWithFirstCourtUseCase(repository, imageUploadRepository);

    await expect(useCase.execute(authenticatedUser, requestWithCourtImage)).rejects.toBeInstanceOf(
      InvalidCourtImageUploadError
    );
    expect(repository.createComplexWithFirstCourt).not.toHaveBeenCalled();
  });

  it('rejects an unknown province without creating identity or owner role side effects', async () => {
    const harness = createRepositoryHarness({ provinceExists: false });
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

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
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

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
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

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

  it('creates a court for an owned complex and persists court services atomically', async () => {
    const harness = createRepositoryHarness();
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

    await expect(
      repository.createOwnedComplexCourt({
        ownerIdentity: {
          sub: 'owner-sub',
          provider: 'Google'
        },
        complexId: 'complex-id',
        court: request.firstCourt
      })
    ).resolves.toEqual(created.firstCourt);

    expect(harness.prisma.$transaction).toHaveBeenCalledTimes(1);
    const transactionClient = harness.transactionClients[0];
    expect(transactionClient.complex.findFirst).toHaveBeenCalledWith({
      where: {
        id: 'complex-id',
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
    expect(transactionClient.court.create).toHaveBeenCalledWith({
      data: {
        complexId: 'complex-id',
        name: 'Court A'
      }
    });
    expect(transactionClient.courtService.createMany).toHaveBeenCalledWith({
      data: [
        { courtId: 'court-id', serviceCatalogId: 'court-service-id' },
        { courtId: 'court-id', serviceCatalogId: 'grass-service-id' }
      ]
    });
  });

  it('persists the optional image upload association when adding one more court', async () => {
    const harness = createRepositoryHarness();
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

    await expect(
      repository.createOwnedComplexCourt({
        ownerIdentity: {
          sub: 'owner-sub',
          provider: 'Google'
        },
        complexId: 'complex-id',
        court: requestWithCourtImage.firstCourt
      })
    ).resolves.toEqual(created.firstCourt);

    const transactionClient = harness.transactionClients[0];
    expect(transactionClient.court.create).toHaveBeenCalledWith({
      data: {
        complexId: 'complex-id',
        name: 'Court A',
        imageUploadId: 'court-image-id'
      }
    });
  });

  it('maps a reused court image unique constraint to a stable validation error', async () => {
    const harness = createRepositoryHarness({
      failCourtCreateWithImageUploadUniqueConstraint: true
    });
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

    await expect(
      repository.createOwnedComplexCourt({
        ownerIdentity: {
          sub: 'owner-sub',
          provider: 'Google'
        },
        complexId: 'complex-id',
        court: requestWithCourtImage.firstCourt
      })
    ).rejects.toBeInstanceOf(InvalidCourtImageUploadError);
  });

  it('rejects add-court when the complex does not belong to the authenticated owner', async () => {
    const harness = createRepositoryHarness({ ownedComplexExists: false });
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

    await expect(
      repository.createOwnedComplexCourt({
        ownerIdentity: {
          sub: 'owner-sub',
          provider: 'Google'
        },
        complexId: 'complex-id',
        court: request.firstCourt
      })
    ).rejects.toBeInstanceOf(ComplexNotFoundForOwnerError);

    const transactionClient = harness.transactionClients[0];
    expect(transactionClient.court.create).not.toHaveBeenCalled();
    expect(transactionClient.courtService.createMany).not.toHaveBeenCalled();
  });

  it('rejects add-court when the selected court services are invalid', async () => {
    const harness = createRepositoryHarness({ courtServicesFound: false });
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

    await expect(
      repository.createOwnedComplexCourt({
        ownerIdentity: {
          sub: 'owner-sub',
          provider: 'Google'
        },
        complexId: 'complex-id',
        court: request.firstCourt
      })
    ).rejects.toBeInstanceOf(InvalidServiceCatalogSelectionError);

    const transactionClient = harness.transactionClients[0];
    expect(transactionClient.court.create).not.toHaveBeenCalled();
    expect(transactionClient.courtService.createMany).not.toHaveBeenCalled();
  });

  it('updates one owned court image and returns a signed image url snapshot', async () => {
    const harness = createRepositoryHarness();
    const fileReadUrl = createFileReadUrlPort();
    const repository = new PrismaComplexRepository(harness.prisma as never, fileReadUrl);

    await expect(
      repository.updateOwnedCourtImage({
        ownerIdentity: {
          sub: 'owner-sub',
          provider: 'Google'
        },
        complexId: 'complex-id',
        courtId: 'court-id',
        imageUploadId: 'court-image-id'
      })
    ).resolves.toEqual({
      id: 'court-id',
      name: 'Court A',
      status: 'ACTIVE',
      availabilityStatus: 'CONFIGURED',
      imageUrl: 'https://signed.example.test/court-image.png'
    });

    const transactionClient = harness.transactionClients[0];
    expect(transactionClient.court.findFirst).toHaveBeenCalledWith({
      where: {
        id: 'court-id',
        deletedAt: null,
        complex: {
          deletedAt: null,
          owner: {
            identities: {
              some: {
                provider: 'Google',
                providerSubject: 'owner-sub'
              }
            }
          }
        }
      },
      select: { id: true }
    });
    expect(transactionClient.court.update).toHaveBeenCalledWith({
      where: { id: 'court-id' },
      data: { imageUploadId: 'court-image-id' },
      select: expect.any(Object)
    });
    expect(fileReadUrl.createReadUrl).toHaveBeenCalledWith(
      'dev/uploads/court-image/owner-sub/2026/06/court-image.png'
    );
  });

  it('updates one owned court image even when the provided complex id is stale', async () => {
    const harness = createRepositoryHarness();
    const fileReadUrl = createFileReadUrlPort();
    const repository = new PrismaComplexRepository(harness.prisma as never, fileReadUrl);

    await expect(
      repository.updateOwnedCourtImage({
        ownerIdentity: {
          sub: 'owner-sub',
          provider: 'Google'
        },
        complexId: 'stale-complex-id',
        courtId: 'court-id',
        imageUploadId: 'court-image-id'
      })
    ).resolves.toEqual({
      id: 'court-id',
      name: 'Court A',
      status: 'ACTIVE',
      availabilityStatus: 'CONFIGURED',
      imageUrl: 'https://signed.example.test/court-image.png'
    });

    expect(harness.transactionClients[0].court.update).toHaveBeenCalledWith({
      where: { id: 'court-id' },
      data: { imageUploadId: 'court-image-id' },
      select: expect.any(Object)
    });
  });

  it('updates one owned court image and degrades imageUrl to null when read url signing fails', async () => {
    const harness = createRepositoryHarness();
    const fileReadUrl = createFileReadUrlPort({
      failingObjectKeys: ['dev/uploads/court-image/owner-sub/2026/06/court-image.png']
    });
    const repository = new PrismaComplexRepository(harness.prisma as never, fileReadUrl);

    await expect(
      repository.updateOwnedCourtImage({
        ownerIdentity: {
          sub: 'owner-sub',
          provider: 'Google'
        },
        complexId: 'complex-id',
        courtId: 'court-id',
        imageUploadId: 'court-image-id'
      })
    ).resolves.toEqual({
      id: 'court-id',
      name: 'Court A',
      status: 'ACTIVE',
      availabilityStatus: 'CONFIGURED',
      imageUrl: null
    });

    expect(harness.transactionClients[0].court.update).toHaveBeenCalledTimes(1);
    expect(fileReadUrl.createReadUrl).toHaveBeenCalledWith(
      'dev/uploads/court-image/owner-sub/2026/06/court-image.png'
    );
  });

  it('rejects update-court-image when the court does not belong to the authenticated owner', async () => {
    const harness = createRepositoryHarness({ ownedCourtExists: false });
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

    await expect(
      repository.updateOwnedCourtImage({
        ownerIdentity: {
          sub: 'owner-sub',
          provider: 'Google'
        },
        complexId: 'complex-id',
        courtId: 'court-id',
        imageUploadId: 'court-image-id'
      })
    ).rejects.toBeInstanceOf(CourtNotFoundForOwnerError);

    const transactionClient = harness.transactionClients[0];
    expect(transactionClient.court.update).not.toHaveBeenCalled();
  });

  it('rolls back add-court writes when court service persistence fails after court creation', async () => {
    const harness = createRepositoryHarness({ failCourtServiceCreate: true });
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

    await expect(
      repository.createOwnedComplexCourt({
        ownerIdentity: {
          sub: 'owner-sub',
          provider: 'Google'
        },
        complexId: 'complex-id',
        court: request.firstCourt
      })
    ).rejects.toThrow('court-service-create-failed');

    expect(harness.prisma.$transaction).toHaveBeenCalledTimes(1);
    const transactionClient = harness.transactionClients[0];
    expect(transactionClient.court.create).toHaveBeenCalledTimes(1);
    expect(transactionClient.courtService.createMany).toHaveBeenCalledTimes(1);
    expect(harness.state.courts).toEqual([]);
    expect(harness.state.courtServices).toEqual([]);
  });

  it('rolls back owner role and write state when complex creation fails after validation', async () => {
    const harness = createRepositoryHarness({ failComplexCreate: true });
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

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
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

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

  it('returns only complexes owned by the authenticated identity in my hub', async () => {
    const harness = createMyHubRepositoryHarness();
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

    await expect(
      repository.getMyComplexHub({
        ownerIdentity: {
          sub: 'owner-sub',
          provider: 'Google'
        }
      })
    ).resolves.toEqual(myHub);

    expect(harness.findMany).toHaveBeenCalledWith({
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
      select: expect.any(Object)
    });
  });

  it('returns an empty my hub when the owner has no complexes and falls back to Cognito provider', async () => {
    const harness = createMyHubRepositoryHarness();
    const repository = new PrismaComplexRepository(harness.prisma as never, createFileReadUrlPort());

    await expect(
      repository.getMyComplexHub({
        ownerIdentity: {
          sub: 'owner-sub-without-complexes'
        }
      })
    ).resolves.toEqual({ complexes: [] });

    expect(harness.findMany).toHaveBeenCalledWith({
      where: {
        deletedAt: null,
        owner: {
          identities: {
            some: {
              provider: 'Cognito',
              providerSubject: 'owner-sub-without-complexes'
            }
          }
        }
      },
      orderBy: [{ createdAt: 'asc' }, { name: 'asc' }],
      select: expect.any(Object)
    });
  });

  it('returns my hub with imageUrl null when read url signing fails for one owned court', async () => {
    const harness = createMyHubRepositoryHarness({ includeCourtImages: true });
    const fileReadUrl = createFileReadUrlPort({
      failingObjectKeys: ['dev/uploads/court-image/owner-sub/2026/06/court-a.png']
    });
    const repository = new PrismaComplexRepository(harness.prisma as never, fileReadUrl);

    await expect(
      repository.getMyComplexHub({
        ownerIdentity: {
          sub: 'owner-sub',
          provider: 'Google'
        }
      })
    ).resolves.toEqual({
      complexes: [
        {
          ...myHub.complexes[0],
          courts: [
            {
              ...myHub.complexes[0].courts[0],
              imageUrl: null
            },
            {
              ...myHub.complexes[0].courts[1],
              imageUrl: 'https://signed.example.test/court-b.png'
            }
          ]
        }
      ]
    });

    expect(fileReadUrl.createReadUrl).toHaveBeenNthCalledWith(
      1,
      'dev/uploads/court-image/owner-sub/2026/06/court-a.png'
    );
    expect(fileReadUrl.createReadUrl).toHaveBeenNthCalledWith(
      2,
      'dev/uploads/court-image/owner-sub/2026/06/court-b.png'
    );
  });
});

function createMyHubRepositoryHarness(options?: { includeCourtImages?: boolean }) {
  const records = [
    {
      ownerProvider: 'Google',
      ownerSub: 'owner-sub',
      complex: {
        id: 'complex-id',
        name: 'North Sports Center',
        address: '123 Main Street',
        provinceId: 'province-id',
        cantonId: 'canton-id',
        latitude: 9.935,
        longitude: -84.091,
        status: 'ACTIVE',
        createdAt: new Date('2026-06-20T00:00:00.000Z'),
        courts: [
          {
            id: 'court-configured-id',
            name: 'Court A',
            status: 'ACTIVE',
            imageUpload: options?.includeCourtImages
              ? {
                  objectKey: 'dev/uploads/court-image/owner-sub/2026/06/court-a.png'
                }
              : null,
            availability: { id: 'availability-id' }
          },
          {
            id: 'court-pending-id',
            name: 'Court B',
            status: 'ACTIVE',
            imageUpload: options?.includeCourtImages
              ? {
                  objectKey: 'dev/uploads/court-image/owner-sub/2026/06/court-b.png'
                }
              : null,
            availability: null
          }
        ]
      }
    },
    {
      ownerProvider: 'Google',
      ownerSub: 'different-owner-sub',
      complex: {
        id: 'foreign-complex-id',
        name: 'Foreign Sports Center',
        address: '456 Other Street',
        provinceId: 'province-id',
        cantonId: 'canton-id',
        latitude: 10.0,
        longitude: -84.1,
        status: 'ACTIVE',
        createdAt: new Date('2026-06-21T00:00:00.000Z'),
        courts: [
          {
            id: 'foreign-court-id',
            name: 'Foreign Court',
            status: 'ACTIVE',
            imageUpload: options?.includeCourtImages
              ? {
                  objectKey: 'dev/uploads/court-image/different-owner-sub/2026/06/foreign-court.png'
                }
              : null,
            availability: { id: 'foreign-availability-id' }
          }
        ]
      }
    }
  ];

  const findMany = jest.fn().mockImplementation(async ({ where }: { where: { deletedAt: null; owner: { identities: { some: { provider: string; providerSubject: string } } } } }) => {
    const ownerFilter = where.owner.identities.some;

    return records
      .filter(
        (record) =>
          record.ownerProvider === ownerFilter.provider &&
          record.ownerSub === ownerFilter.providerSubject
      )
      .map((record) => record.complex);
  });

  return {
    findMany,
    prisma: {
      $transaction: jest.fn(),
      complex: {
        findMany
      }
    }
  };
}

function createComplexRepositoryDouble(
  overrides: Partial<jest.Mocked<IComplexRepository>> = {}
): jest.Mocked<IComplexRepository> {
  return {
    findCourtIdByImageUploadId: jest.fn().mockResolvedValue(null),
    createComplexWithFirstCourt: jest.fn(),
    createOwnedComplexCourt: jest.fn(),
    updateOwnedCourtImage: jest.fn(),
    getMyComplexHub: jest.fn(),
    ...overrides
  };
}

function createFileReadUrlPort(options?: {
  failingObjectKeys?: string[];
}): jest.Mocked<IFileReadUrlPort> {
  const failingObjectKeys = new Set(options?.failingObjectKeys ?? []);

  return {
    createReadUrl: jest.fn().mockImplementation(async (objectKey: string) => {
      if (failingObjectKeys.has(objectKey)) {
        throw new Error(`read-url-failed:${objectKey}`);
      }

      const encoded = encodeURIComponent(objectKey.split('/').pop() ?? 'court-image.png');
      return `https://signed.example.test/${encoded}`;
    })
  };
}

function createImageUploadRepository(options?: {
  imageUpload?: ImageUploadEntity | null;
}): jest.Mocked<IImageUploadRepository> {
  return {
    findById: jest.fn().mockResolvedValue(options?.imageUpload ?? null),
    saveConfirmedUpload: jest.fn(),
    listRecent: jest.fn()
  };
}
