import type { ExecutionContext } from '@nestjs/common';
import { ListUsersUseCase } from '@/modules/users/application/use-cases/list-users.use-case';
import { DeactivateUserUseCase } from '@/modules/users/application/use-cases/deactivate-user.use-case';
import { SyncAuthenticatedUserUseCase } from '@/modules/users/application/use-cases/sync-authenticated-user.use-case';
import type { UserProfileService } from '@/modules/users/application/services/user-profile.service';
import type { UpdateMyProfileImageUseCase } from '@/modules/users/application/use-cases/update-my-profile-image.use-case';
import { UserEntity } from '@/modules/users/domain/entities/user.entity';
import { AdminRoleRequiredError } from '@/modules/users/domain/errors/admin-role-required.error';
import { UserAccountInactiveError } from '@/modules/users/domain/errors/user-account-inactive.error';
import { UserEmailAlreadyExistsError } from '@/modules/users/domain/errors/user-email-already-exists.error';
import { UserNotFoundError } from '@/modules/users/domain/errors/user-not-found.error';
import type { IUserRepository } from '@/modules/users/domain/repositories/user.repository';
import { UserListUnavailableError } from '@/modules/users/infrastructure/errors/user-list-unavailable.error';
import { UserMapper } from '@/modules/users/infrastructure/mappers/user.mapper';
import { PrismaUserRepository } from '@/modules/users/infrastructure/persistence/prisma-user.repository';
import { UsersController } from '@/modules/users/interfaces/http/controllers/users.controller';
import { ActiveUserAccountGuard } from '@/modules/users/interfaces/http/guards/active-user-account.guard';

describe('users module behavior', () => {
  function createProfileService(): UserProfileService {
    return {
      render: jest.fn(async (user: UserEntity) => user.toProfile())
    } as unknown as UserProfileService;
  }

  function createUniqueConstraintError(): Error & { code: 'P2002' } {
    return Object.assign(new Error('Unique constraint failed'), {
      code: 'P2002' as const
    });
  }

  function createHttpContext(request: unknown): ExecutionContext {
    return {
      switchToHttp: () => ({
        getRequest: () => request
      })
    } as unknown as ExecutionContext;
  }

  const googleIdentity = {
    provider: 'Google',
    providerSubject: 'cognito-sub'
  };
  const persistenceUser = {
    id: 'user-id',
    email: 'user@example.test',
    name: 'User Name',
    pictureUrl: 'https://example.test/avatar.png',
    status: 'ACTIVE' as const,
    identities: [googleIdentity],
    roles: [{ role: 'PLAYER' as const }]
  };
  const userProfile = {
    id: 'user-id',
    cognitoSub: 'cognito-sub',
    email: 'user@example.test',
    name: 'User Name',
    pictureUrl: 'https://example.test/avatar.png',
    provider: 'Google',
    roles: ['PLAYER'],
    status: 'ACTIVE'
  };

  it('converts user entities to API-safe profiles', () => {
    const entity = UserEntity.fromPersistence({
      ...persistenceUser,
      name: null,
      pictureUrl: null,
      currentIdentity: null,
      roles: ['PLAYER']
    });

    expect(entity.toProfile()).toEqual({
      id: 'user-id',
      cognitoSub: undefined,
      email: 'user@example.test',
      name: undefined,
      pictureUrl: undefined,
      provider: undefined,
      roles: ['PLAYER'],
      status: 'ACTIVE'
    });
  });

  it('exposes an empty roles array when no roles are present on the entity', () => {
    const entity = UserEntity.fromPersistence({
      id: 'user-id',
      email: 'user@example.test',
      name: null,
      pictureUrl: null,
      status: 'ACTIVE',
      currentIdentity: null
    });

    expect(entity.toProfile().roles).toEqual([]);
  });

  it('exposes OWNER role when the entity carries an OWNER role record', () => {
    const entity = UserEntity.fromPersistence({
      id: 'user-id',
      email: 'user@example.test',
      status: 'ACTIVE',
      roles: ['OWNER']
    });

    expect(entity.toProfile().roles).toEqual(['OWNER']);
  });

  it('maps Prisma users to domain entities', () => {
    expect(UserMapper.toDomain(persistenceUser).toProfile()).toEqual(userProfile);
  });

  it('syncs authenticated users through the repository port', async () => {
    const entity = UserEntity.fromPersistence({
      ...persistenceUser,
      currentIdentity: googleIdentity,
      roles: ['PLAYER']
    });
    const repository = {
      syncAuthenticatedUser: jest.fn().mockResolvedValue(entity),
      findByCognitoSub: jest.fn(),
      replaceProfileImage: jest.fn(),
      list: jest.fn(),
      deactivateById: jest.fn()
    } satisfies IUserRepository;
    const useCase = new SyncAuthenticatedUserUseCase(
      repository,
      createProfileService()
    );

    await expect(
      useCase.execute({
        sub: 'cognito-sub',
        email: 'user@example.test',
        emailVerified: true,
        name: 'User Name',
        pictureUrl: 'https://example.test/avatar.png',
        provider: 'Google',
        groups: ['admin']
      })
    ).resolves.toEqual(userProfile);
    expect(repository.syncAuthenticatedUser).toHaveBeenCalledWith({
      cognitoSub: 'cognito-sub',
      email: 'user@example.test',
      emailVerified: true,
      name: 'User Name',
      pictureUrl: 'https://example.test/avatar.png',
      provider: 'Google'
    });
  });

  it('blocks synchronized inactive users from normal profile flow', async () => {
    const inactiveUser = UserEntity.fromPersistence({
      ...persistenceUser,
      status: 'INACTIVE',
      currentIdentity: googleIdentity,
      roles: ['PLAYER']
    });
    const profileService = createProfileService();
    const repository = {
      syncAuthenticatedUser: jest.fn().mockResolvedValue(inactiveUser),
      findByCognitoSub: jest.fn(),
      replaceProfileImage: jest.fn(),
      list: jest.fn(),
      deactivateById: jest.fn()
    } satisfies IUserRepository;
    const useCase = new SyncAuthenticatedUserUseCase(repository, profileService);

    await expect(
      useCase.execute({ sub: 'cognito-sub', groups: [] })
    ).rejects.toBeInstanceOf(UserAccountInactiveError);
    expect(profileService.render).not.toHaveBeenCalled();
  });

  it('blocks inactive local users from guarded operational routes', async () => {
    const inactiveUser = UserEntity.fromPersistence({
      ...persistenceUser,
      status: 'INACTIVE',
      currentIdentity: googleIdentity,
      roles: ['PLAYER']
    });
    const repository = {
      syncAuthenticatedUser: jest.fn(),
      findByCognitoSub: jest.fn().mockResolvedValue(inactiveUser),
      replaceProfileImage: jest.fn(),
      list: jest.fn(),
      deactivateById: jest.fn()
    } satisfies IUserRepository;
    const guard = new ActiveUserAccountGuard(repository);

    await expect(
      guard.canActivate(createHttpContext({ user: { sub: 'cognito-sub', groups: [] } }))
    ).rejects.toBeInstanceOf(UserAccountInactiveError);
    expect(repository.findByCognitoSub).toHaveBeenCalledWith('cognito-sub');
  });

  it('allows guarded operational routes when no local user exists yet', async () => {
    const repository = {
      syncAuthenticatedUser: jest.fn(),
      findByCognitoSub: jest.fn().mockResolvedValue(null),
      replaceProfileImage: jest.fn(),
      list: jest.fn(),
      deactivateById: jest.fn()
    } satisfies IUserRepository;
    const guard = new ActiveUserAccountGuard(repository);

    await expect(
      guard.canActivate(createHttpContext({ user: { sub: 'new-cognito-sub', groups: [] } }))
    ).resolves.toBe(true);
    expect(repository.findByCognitoSub).toHaveBeenCalledWith('new-cognito-sub');
  });

  it('returns roles from userRole records after sync', async () => {
    const originalDemoOwnerEmails = process.env.DEMO_OWNER_EMAILS;
    const originalDemoOwnerSubs = process.env.DEMO_OWNER_SUBS;
    delete process.env.DEMO_OWNER_EMAILS;
    delete process.env.DEMO_OWNER_SUBS;
    const prisma = {
      user: {
        create: jest.fn().mockResolvedValue(persistenceUser),
        findUnique: jest.fn().mockResolvedValue(null),
        update: jest.fn(),
        findMany: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
      },
      userRole: {
        findUnique: jest.fn(),
        upsert: jest.fn(),
        findMany: jest.fn().mockResolvedValue([{ role: 'OWNER' }])
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    const entity = await repository.syncAuthenticatedUser({
      cognitoSub: 'cognito-sub',
      name: 'User Name'
    });

    expect(entity.toProfile().roles).toEqual(['OWNER']);
    expect(prisma.userRole.findMany).toHaveBeenCalledWith({
      where: { userId: 'user-id' },
      select: { role: true }
    });
    process.env.DEMO_OWNER_EMAILS = originalDemoOwnerEmails;
    process.env.DEMO_OWNER_SUBS = originalDemoOwnerSubs;
  });

  it('creates authenticated users with a linked identity', async () => {
    const originalDemoOwnerEmails = process.env.DEMO_OWNER_EMAILS;
    const originalDemoOwnerSubs = process.env.DEMO_OWNER_SUBS;
    delete process.env.DEMO_OWNER_EMAILS;
    delete process.env.DEMO_OWNER_SUBS;
    const prisma = {
      user: {
        create: jest.fn().mockResolvedValue(persistenceUser),
        findUnique: jest.fn().mockResolvedValue(null),
        update: jest.fn(),
        findMany: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
      },
      userRole: {
        findUnique: jest.fn(),
        upsert: jest.fn(),
        findMany: jest.fn().mockResolvedValue([])
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(
      repository.syncAuthenticatedUser({
        cognitoSub: 'cognito-sub',
        name: 'User Name'
      })
    ).resolves.toEqual(expect.any(UserEntity));

    expect(prisma.userIdentity.findUnique).toHaveBeenCalledWith({
      where: {
        provider_providerSubject: {
          provider: 'Cognito',
          providerSubject: 'cognito-sub'
        }
      },
      include: {
        user: {
          include: {
            identities: true
          }
        }
      }
    });
    expect(prisma.user.create).toHaveBeenCalledWith({
      data: {
        email: 'cognito-sub@unknown.local',
        name: 'User Name',
        identities: {
          create: {
            provider: 'Cognito',
            providerSubject: 'cognito-sub'
          }
        }
      },
      include: {
        identities: true
      }
    });
    expect(prisma.userRole.findUnique).not.toHaveBeenCalled();
    expect(prisma.userRole.upsert).not.toHaveBeenCalled();
    process.env.DEMO_OWNER_EMAILS = originalDemoOwnerEmails;
    process.env.DEMO_OWNER_SUBS = originalDemoOwnerSubs;
  });

  it('links verified email users when a new provider signs in', async () => {
    const originalDemoOwnerEmails = process.env.DEMO_OWNER_EMAILS;
    const originalDemoOwnerSubs = process.env.DEMO_OWNER_SUBS;
    delete process.env.DEMO_OWNER_EMAILS;
    delete process.env.DEMO_OWNER_SUBS;
    const linkedUser = {
      ...persistenceUser,
      identities: [
        googleIdentity,
        {
          provider: 'Cognito',
          providerSubject: 'new-cognito-sub'
        }
      ]
    };
    const prisma = {
      user: {
        create: jest.fn(),
        findUnique: jest.fn().mockResolvedValue(persistenceUser),
        update: jest.fn().mockResolvedValue(linkedUser),
        findMany: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
      },
      userRole: {
        findUnique: jest.fn(),
        upsert: jest.fn(),
        findMany: jest.fn().mockResolvedValue([])
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(
      repository.syncAuthenticatedUser({
        cognitoSub: 'new-cognito-sub',
        email: 'user@example.test',
        emailVerified: true,
        name: 'User Name',
        provider: 'Cognito'
      })
    ).resolves.toEqual(expect.any(UserEntity));

    expect(prisma.user.findUnique).toHaveBeenCalledWith({
      where: { email: 'user@example.test' },
      include: {
        identities: true
      }
    });
    expect(prisma.user.update).toHaveBeenCalledWith({
      where: { id: 'user-id' },
      data: {
        email: 'user@example.test',
        name: 'User Name',
        identities: {
          create: {
            provider: 'Cognito',
            providerSubject: 'new-cognito-sub',
            emailAtLogin: 'user@example.test'
          }
        }
      },
      include: {
        identities: true
      }
    });
    expect(prisma.user.create).not.toHaveBeenCalled();
    process.env.DEMO_OWNER_EMAILS = originalDemoOwnerEmails;
    process.env.DEMO_OWNER_SUBS = originalDemoOwnerSubs;
  });

  it('re-reads and succeeds when user creation loses a P2002 race to the same identity', async () => {
    const originalDemoOwnerEmails = process.env.DEMO_OWNER_EMAILS;
    const originalDemoOwnerSubs = process.env.DEMO_OWNER_SUBS;
    delete process.env.DEMO_OWNER_EMAILS;
    delete process.env.DEMO_OWNER_SUBS;
    const linkedUser = {
      ...persistenceUser,
      identities: [
        {
          provider: 'Cognito',
          providerSubject: 'cognito-sub'
        }
      ]
    };
    const prisma = {
      user: {
        create: jest
          .fn()
          .mockRejectedValueOnce(createUniqueConstraintError()),
        findUnique: jest.fn().mockResolvedValue(null),
        update: jest.fn().mockResolvedValue(linkedUser),
        findMany: jest.fn()
      },
      userIdentity: {
        findUnique: jest
          .fn()
          .mockResolvedValueOnce(null)
          .mockResolvedValueOnce({
            userId: 'user-id',
            user: linkedUser
          })
          .mockResolvedValueOnce({
            userId: 'user-id',
            user: linkedUser
          })
      },
      userRole: {
        findUnique: jest.fn(),
        upsert: jest.fn(),
        deleteMany: jest.fn(),
        findMany: jest.fn().mockResolvedValue([])
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(
      repository.syncAuthenticatedUser({
        cognitoSub: 'cognito-sub',
        email: 'user@example.test',
        emailVerified: true,
        name: 'User Name',
        provider: 'Cognito'
      })
    ).resolves.toEqual(expect.any(UserEntity));

    expect(prisma.user.create).toHaveBeenCalledTimes(1);
    expect(prisma.user.update).toHaveBeenCalledWith({
      where: { id: 'user-id' },
      data: {
        email: 'user@example.test',
        name: 'User Name'
      },
      include: {
        identities: true
      }
    });
    process.env.DEMO_OWNER_EMAILS = originalDemoOwnerEmails;
    process.env.DEMO_OWNER_SUBS = originalDemoOwnerSubs;
  });

  it('re-reads and succeeds when verified email linking loses a P2002 race to identity creation', async () => {
    const originalDemoOwnerEmails = process.env.DEMO_OWNER_EMAILS;
    const originalDemoOwnerSubs = process.env.DEMO_OWNER_SUBS;
    delete process.env.DEMO_OWNER_EMAILS;
    delete process.env.DEMO_OWNER_SUBS;
    const linkedUser = {
      ...persistenceUser,
      identities: [
        googleIdentity,
        {
          provider: 'Cognito',
          providerSubject: 'new-cognito-sub'
        }
      ]
    };
    const prisma = {
      user: {
        create: jest.fn(),
        findUnique: jest
          .fn()
          .mockResolvedValueOnce(persistenceUser),
        update: jest
          .fn()
          .mockRejectedValueOnce(createUniqueConstraintError())
          .mockResolvedValueOnce(linkedUser),
        findMany: jest.fn()
      },
      userIdentity: {
        findUnique: jest
          .fn()
          .mockResolvedValueOnce(null)
          .mockResolvedValueOnce({
            userId: 'user-id',
            user: linkedUser
          })
          .mockResolvedValueOnce({
            userId: 'user-id',
            user: linkedUser
          })
      },
      userRole: {
        findUnique: jest.fn(),
        upsert: jest.fn(),
        deleteMany: jest.fn(),
        findMany: jest.fn().mockResolvedValue([])
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(
      repository.syncAuthenticatedUser({
        cognitoSub: 'new-cognito-sub',
        email: 'user@example.test',
        emailVerified: true,
        name: 'User Name',
        provider: 'Cognito'
      })
    ).resolves.toEqual(expect.any(UserEntity));

    expect(prisma.user.update).toHaveBeenNthCalledWith(1, {
      where: { id: 'user-id' },
      data: {
        email: 'user@example.test',
        name: 'User Name',
        identities: {
          create: {
            provider: 'Cognito',
            providerSubject: 'new-cognito-sub',
            emailAtLogin: 'user@example.test'
          }
        }
      },
      include: {
        identities: true
      }
    });
    expect(prisma.user.update).toHaveBeenNthCalledWith(2, {
      where: { id: 'user-id' },
      data: {
        email: 'user@example.test',
        name: 'User Name'
      },
      include: {
        identities: true
      }
    });
    process.env.DEMO_OWNER_EMAILS = originalDemoOwnerEmails;
    process.env.DEMO_OWNER_SUBS = originalDemoOwnerSubs;
  });

  it('rejects unverified email collisions without linking identities', async () => {
    const prisma = {
      user: {
        create: jest.fn(),
        findUnique: jest.fn().mockResolvedValue(persistenceUser),
        update: jest.fn(),
        findMany: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
      },
      userRole: {
        findUnique: jest.fn(),
        upsert: jest.fn(),
        findMany: jest.fn()
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(
      repository.syncAuthenticatedUser({
        cognitoSub: 'new-cognito-sub',
        email: 'user@example.test',
        emailVerified: false,
        provider: 'Cognito'
      })
    ).rejects.toBeInstanceOf(UserEmailAlreadyExistsError);
    expect(prisma.user.update).not.toHaveBeenCalled();
    expect(prisma.user.create).not.toHaveBeenCalled();
  });

  it('provisions the OWNER role during sync when the demo email allowlist matches and email is verified', async () => {
    const originalDemoOwnerEmails = process.env.DEMO_OWNER_EMAILS;
    const originalDemoOwnerSubs = process.env.DEMO_OWNER_SUBS;
    process.env.DEMO_OWNER_EMAILS = 'owner@example.test,another@example.test';
    delete process.env.DEMO_OWNER_SUBS;
    const prisma = {
      user: {
        create: jest.fn().mockResolvedValue(persistenceUser),
        findUnique: jest.fn().mockResolvedValue(null),
        update: jest.fn(),
        findMany: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
      },
      userRole: {
        findUnique: jest.fn(),
        upsert: jest.fn().mockResolvedValue({ id: 'owner-role-id' }),
        deleteMany: jest.fn(),
        findMany: jest.fn().mockResolvedValue([{ role: 'OWNER' }])
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await repository.syncAuthenticatedUser({
      cognitoSub: 'cognito-sub',
      email: 'OWNER@example.test',
      emailVerified: true,
      name: 'User Name'
    });

    expect(prisma.userRole.upsert).toHaveBeenCalledWith({
      where: {
        userId_role: {
          userId: 'user-id',
          role: 'OWNER'
        }
      },
      create: {
        userId: 'user-id',
        role: 'OWNER'
      },
      update: {}
    });
    expect(prisma.userRole.deleteMany).not.toHaveBeenCalled();
    process.env.DEMO_OWNER_EMAILS = originalDemoOwnerEmails;
    process.env.DEMO_OWNER_SUBS = originalDemoOwnerSubs;
  });

  it('does not provision OWNER by email when the email is not verified', async () => {
    const originalDemoOwnerEmails = process.env.DEMO_OWNER_EMAILS;
    const originalDemoOwnerSubs = process.env.DEMO_OWNER_SUBS;
    process.env.DEMO_OWNER_EMAILS = 'owner@example.test';
    delete process.env.DEMO_OWNER_SUBS;
    const prisma = {
      user: {
        create: jest.fn().mockResolvedValue(persistenceUser),
        findUnique: jest.fn().mockResolvedValue(null),
        update: jest.fn(),
        findMany: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
      },
      userRole: {
        findUnique: jest.fn(),
        upsert: jest.fn(),
        deleteMany: jest.fn(),
        findMany: jest.fn().mockResolvedValue([])
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await repository.syncAuthenticatedUser({
      cognitoSub: 'cognito-sub',
      email: 'owner@example.test',
      emailVerified: false,
      name: 'User Name'
    });

    expect(prisma.userRole.upsert).not.toHaveBeenCalled();
    process.env.DEMO_OWNER_EMAILS = originalDemoOwnerEmails;
    process.env.DEMO_OWNER_SUBS = originalDemoOwnerSubs;
  });

  it('does not provision OWNER by email when the email verification claim is missing', async () => {
    const originalDemoOwnerEmails = process.env.DEMO_OWNER_EMAILS;
    const originalDemoOwnerSubs = process.env.DEMO_OWNER_SUBS;
    process.env.DEMO_OWNER_EMAILS = 'owner@example.test';
    delete process.env.DEMO_OWNER_SUBS;
    const prisma = {
      user: {
        create: jest.fn().mockResolvedValue(persistenceUser),
        findUnique: jest.fn().mockResolvedValue(null),
        update: jest.fn(),
        findMany: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
      },
      userRole: {
        findUnique: jest.fn(),
        upsert: jest.fn(),
        deleteMany: jest.fn(),
        findMany: jest.fn().mockResolvedValue([])
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await repository.syncAuthenticatedUser({
      cognitoSub: 'cognito-sub',
      email: 'owner@example.test',
      name: 'User Name'
    });

    expect(prisma.userRole.upsert).not.toHaveBeenCalled();
    process.env.DEMO_OWNER_EMAILS = originalDemoOwnerEmails;
    process.env.DEMO_OWNER_SUBS = originalDemoOwnerSubs;
  });

  it('provisions the OWNER role during sync when the demo sub allowlist matches', async () => {
    const originalDemoOwnerEmails = process.env.DEMO_OWNER_EMAILS;
    const originalDemoOwnerSubs = process.env.DEMO_OWNER_SUBS;
    delete process.env.DEMO_OWNER_EMAILS;
    process.env.DEMO_OWNER_SUBS = 'cognito-sub,another-sub';
    const prisma = {
      user: {
        create: jest.fn().mockResolvedValue(persistenceUser),
        findUnique: jest.fn().mockResolvedValue(null),
        update: jest.fn(),
        findMany: jest.fn()
      },
      userIdentity: {
        findUnique: jest.fn().mockResolvedValue(null)
      },
      userRole: {
        findUnique: jest.fn(),
        upsert: jest.fn().mockResolvedValue({ id: 'owner-role-id' }),
        deleteMany: jest.fn(),
        findMany: jest.fn().mockResolvedValue([{ role: 'OWNER' }])
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await repository.syncAuthenticatedUser({
      cognitoSub: 'cognito-sub',
      email: 'owner@example.test',
      name: 'User Name'
    });

    expect(prisma.userRole.upsert).toHaveBeenCalledTimes(1);
    process.env.DEMO_OWNER_EMAILS = originalDemoOwnerEmails;
    process.env.DEMO_OWNER_SUBS = originalDemoOwnerSubs;
  });

  it('finds users by Cognito subject through identities', async () => {
    const identity = {
      ...googleIdentity,
      user: persistenceUser
    };
    const prisma = {
      user: {
        create: jest.fn(),
        findUnique: jest.fn(),
        findMany: jest.fn()
      },
      userIdentity: {
        findFirst: jest.fn().mockResolvedValueOnce(identity)
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    const user = await repository.findByCognitoSub('cognito-sub');

    expect(user?.toProfile()).toEqual(userProfile);
    expect(prisma.userIdentity.findFirst).toHaveBeenCalledWith({
      where: { providerSubject: 'cognito-sub' },
      include: {
        user: {
          include: {
            identities: true,
            roles: true
          }
        }
      }
    });
  });

  it('returns null when a Cognito subject is unknown', async () => {
    const prisma = {
      user: {
        create: jest.fn(),
        findUnique: jest.fn(),
        findMany: jest.fn()
      },
      userIdentity: {
        findFirst: jest.fn().mockResolvedValueOnce(null)
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(repository.findByCognitoSub('missing-sub')).resolves.toBeNull();
  });

  it('wraps Prisma failures when loading an admin by Cognito subject', async () => {
    const prisma = {
      user: {
        create: jest.fn(),
        findUnique: jest.fn(),
        findMany: jest.fn()
      },
      userIdentity: {
        findFirst: jest.fn().mockRejectedValueOnce(new Error('database unavailable'))
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(repository.findByCognitoSub('admin-sub')).rejects.toBeInstanceOf(
      UserListUnavailableError
    );
  });

  it('lists synchronized users through the repository port', async () => {
    const entity = UserEntity.fromPersistence({
      ...persistenceUser,
      currentIdentity: googleIdentity,
      roles: ['PLAYER']
    });
    const admin = UserEntity.fromPersistence({
      id: 'admin-id',
      email: 'admin@example.test',
      status: 'ACTIVE',
      roles: ['ADMIN']
    });
    const repository = {
      syncAuthenticatedUser: jest.fn(),
      findByCognitoSub: jest.fn().mockResolvedValue(admin),
      replaceProfileImage: jest.fn(),
      list: jest.fn().mockResolvedValue([entity]),
      deactivateById: jest.fn()
    } satisfies IUserRepository;
    const useCase = new ListUsersUseCase(repository, createProfileService());

    await expect(
      useCase.execute({ sub: 'admin-sub', groups: [] })
    ).resolves.toEqual([userProfile]);
    expect(repository.findByCognitoSub).toHaveBeenCalledWith('admin-sub');
    expect(repository.list).toHaveBeenCalledTimes(1);
  });

  it('lists synchronized users for admins authenticated through Cognito groups even without a local admin row', async () => {
    const entity = UserEntity.fromPersistence({
      ...persistenceUser,
      currentIdentity: googleIdentity,
      roles: ['PLAYER']
    });
    const repository = {
      syncAuthenticatedUser: jest.fn(),
      findByCognitoSub: jest.fn(),
      replaceProfileImage: jest.fn(),
      list: jest.fn().mockResolvedValue([entity]),
      deactivateById: jest.fn()
    } satisfies IUserRepository;
    const useCase = new ListUsersUseCase(repository, createProfileService());

    await expect(
      useCase.execute({ sub: 'admin-sub', groups: ['Admin'] })
    ).resolves.toEqual([userProfile]);
    expect(repository.findByCognitoSub).not.toHaveBeenCalled();
    expect(repository.list).toHaveBeenCalledTimes(1);
  });

  it('returns an empty users list for administrators when no users exist', async () => {
    const admin = UserEntity.fromPersistence({
      id: 'admin-id',
      email: 'admin@example.test',
      status: 'ACTIVE',
      roles: ['ADMIN']
    });
    const repository = {
      syncAuthenticatedUser: jest.fn(),
      findByCognitoSub: jest.fn().mockResolvedValue(admin),
      replaceProfileImage: jest.fn(),
      list: jest.fn().mockResolvedValue([]),
      deactivateById: jest.fn()
    } satisfies IUserRepository;
    const useCase = new ListUsersUseCase(repository, createProfileService());

    await expect(useCase.execute({ sub: 'admin-sub', groups: [] })).resolves.toEqual(
      []
    );
  });

  it('rejects the users list when the authenticated user is not an administrator', async () => {
    const player = UserEntity.fromPersistence({
      id: 'player-id',
      email: 'player@example.test',
      status: 'ACTIVE',
      roles: ['PLAYER']
    });
    const repository = {
      syncAuthenticatedUser: jest.fn(),
      findByCognitoSub: jest.fn().mockResolvedValue(player),
      replaceProfileImage: jest.fn(),
      list: jest.fn(),
      deactivateById: jest.fn()
    } satisfies IUserRepository;
    const useCase = new ListUsersUseCase(repository, createProfileService());

    await expect(useCase.execute({ sub: 'player-sub', groups: [] })).rejects.toBeInstanceOf(
      AdminRoleRequiredError
    );
    expect(repository.list).not.toHaveBeenCalled();
  });

  it('rejects the users list when the authenticated user has no admin group and no local admin role', async () => {
    const repository = {
      syncAuthenticatedUser: jest.fn(),
      findByCognitoSub: jest.fn().mockResolvedValue(null),
      replaceProfileImage: jest.fn(),
      list: jest.fn(),
      deactivateById: jest.fn()
    } satisfies IUserRepository;
    const useCase = new ListUsersUseCase(repository, createProfileService());

    await expect(useCase.execute({ sub: 'player-sub', groups: ['players'] })).rejects.toBeInstanceOf(
      AdminRoleRequiredError
    );
    expect(repository.list).not.toHaveBeenCalled();
  });

  it('deactivates a user when the actor has a local ADMIN role', async () => {
    const admin = UserEntity.fromPersistence({
      id: 'admin-id',
      email: 'admin@example.test',
      status: 'ACTIVE',
      roles: ['ADMIN']
    });
    const inactiveUser = UserEntity.fromPersistence({
      ...persistenceUser,
      status: 'INACTIVE',
      currentIdentity: googleIdentity,
      roles: ['PLAYER']
    });
    const repository = {
      syncAuthenticatedUser: jest.fn(),
      findByCognitoSub: jest.fn().mockResolvedValue(admin),
      replaceProfileImage: jest.fn(),
      list: jest.fn(),
      deactivateById: jest.fn().mockResolvedValue(inactiveUser)
    } satisfies IUserRepository;
    const useCase = new DeactivateUserUseCase(
      repository,
      createProfileService()
    );

    await expect(
      useCase.execute({ sub: 'admin-sub', groups: [] }, 'user-id')
    ).resolves.toEqual({
      ...userProfile,
      status: 'INACTIVE'
    });
    expect(repository.findByCognitoSub).toHaveBeenCalledWith('admin-sub');
    expect(repository.deactivateById).toHaveBeenCalledWith('user-id');
  });

  it('deactivates a user when the actor has an admin Cognito group', async () => {
    const inactiveUser = UserEntity.fromPersistence({
      ...persistenceUser,
      status: 'INACTIVE',
      currentIdentity: googleIdentity,
      roles: ['PLAYER']
    });
    const repository = {
      syncAuthenticatedUser: jest.fn(),
      findByCognitoSub: jest.fn(),
      replaceProfileImage: jest.fn(),
      list: jest.fn(),
      deactivateById: jest.fn().mockResolvedValue(inactiveUser)
    } satisfies IUserRepository;
    const useCase = new DeactivateUserUseCase(
      repository,
      createProfileService()
    );

    await expect(
      useCase.execute({ sub: 'admin-sub', groups: ['Admin'] }, 'user-id')
    ).resolves.toEqual({
      ...userProfile,
      status: 'INACTIVE'
    });
    expect(repository.findByCognitoSub).not.toHaveBeenCalled();
    expect(repository.deactivateById).toHaveBeenCalledWith('user-id');
  });

  it('rejects user deactivation when the actor is not an administrator', async () => {
    const player = UserEntity.fromPersistence({
      id: 'player-id',
      email: 'player@example.test',
      status: 'ACTIVE',
      roles: ['PLAYER']
    });
    const repository = {
      syncAuthenticatedUser: jest.fn(),
      findByCognitoSub: jest.fn().mockResolvedValue(player),
      replaceProfileImage: jest.fn(),
      list: jest.fn(),
      deactivateById: jest.fn()
    } satisfies IUserRepository;
    const useCase = new DeactivateUserUseCase(
      repository,
      createProfileService()
    );

    await expect(
      useCase.execute({ sub: 'player-sub', groups: [] }, 'user-id')
    ).rejects.toBeInstanceOf(AdminRoleRequiredError);
    expect(repository.deactivateById).not.toHaveBeenCalled();
  });

  it('returns a typed not found error when deactivating an unknown user', async () => {
    const repository = {
      syncAuthenticatedUser: jest.fn(),
      findByCognitoSub: jest.fn(),
      replaceProfileImage: jest.fn(),
      list: jest.fn(),
      deactivateById: jest.fn().mockResolvedValue(null)
    } satisfies IUserRepository;
    const useCase = new DeactivateUserUseCase(
      repository,
      createProfileService()
    );

    await expect(
      useCase.execute({ sub: 'admin-sub', groups: ['admin'] }, 'missing-user-id')
    ).rejects.toBeInstanceOf(UserNotFoundError);
  });

  it('lists users from Prisma by recent updates', async () => {
    const prisma = {
      user: {
        create: jest.fn(),
        findUnique: jest.fn(),
        findMany: jest.fn().mockResolvedValueOnce([persistenceUser])
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(repository.list()).resolves.toHaveLength(1);
    expect(prisma.user.findMany).toHaveBeenCalledWith({
      include: {
        identities: true,
        roles: true
      },
      orderBy: { updatedAt: 'desc' }
    });
  });

  it('updates Prisma user status to INACTIVE without deleting records', async () => {
    const inactivePersistenceUser = {
      ...persistenceUser,
      status: 'INACTIVE' as const
    };
    const prisma = {
      user: {
        findUnique: jest.fn().mockResolvedValueOnce(persistenceUser),
        update: jest.fn().mockResolvedValueOnce(inactivePersistenceUser),
        delete: jest.fn(),
        deleteMany: jest.fn()
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(repository.deactivateById('user-id')).resolves.toEqual(
      expect.any(UserEntity)
    );
    expect(prisma.user.findUnique).toHaveBeenCalledWith({
      where: { id: 'user-id' },
      include: {
        identities: true,
        roles: true
      }
    });
    expect(prisma.user.update).toHaveBeenCalledWith({
      where: { id: 'user-id' },
      data: { status: 'INACTIVE' },
      include: {
        identities: true,
        roles: true
      }
    });
    expect(prisma.user.delete).not.toHaveBeenCalled();
    expect(prisma.user.deleteMany).not.toHaveBeenCalled();
  });

  it('returns the current inactive user without updating Prisma again', async () => {
    const inactivePersistenceUser = {
      ...persistenceUser,
      status: 'INACTIVE' as const
    };
    const prisma = {
      user: {
        findUnique: jest.fn().mockResolvedValueOnce(inactivePersistenceUser),
        update: jest.fn(),
        delete: jest.fn(),
        deleteMany: jest.fn()
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    const user = await repository.deactivateById('user-id');

    expect(user?.toProfile()).toEqual({
      ...userProfile,
      status: 'INACTIVE'
    });
    expect(prisma.user.update).not.toHaveBeenCalled();
    expect(prisma.user.delete).not.toHaveBeenCalled();
    expect(prisma.user.deleteMany).not.toHaveBeenCalled();
  });

  it('returns null when Prisma cannot find a user to deactivate', async () => {
    const prisma = {
      user: {
        findUnique: jest.fn().mockResolvedValueOnce(null),
        update: jest.fn(),
        delete: jest.fn(),
        deleteMany: jest.fn()
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(repository.deactivateById('missing-user-id')).resolves.toBeNull();
    expect(prisma.user.update).not.toHaveBeenCalled();
    expect(prisma.user.delete).not.toHaveBeenCalled();
    expect(prisma.user.deleteMany).not.toHaveBeenCalled();
  });

  it('wraps Prisma failures when listing users', async () => {
    const prisma = {
      user: {
        create: jest.fn(),
        findUnique: jest.fn(),
        findMany: jest.fn().mockRejectedValueOnce(new Error('database unavailable'))
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(repository.list()).rejects.toBeInstanceOf(UserListUnavailableError);
  });

  it('delegates the current user endpoint to the sync use case', async () => {
    const listUsers = {
      execute: jest.fn()
    } as unknown as ListUsersUseCase;
    const syncAuthenticatedUser = {
      execute: jest.fn().mockResolvedValue(userProfile)
    } as unknown as SyncAuthenticatedUserUseCase;
    const controller = new UsersController(
      listUsers,
      syncAuthenticatedUser,
      { execute: jest.fn() } as unknown as UpdateMyProfileImageUseCase,
      { execute: jest.fn() } as unknown as DeactivateUserUseCase
    );
    const currentUser = {
      sub: 'cognito-sub',
      groups: []
    };

    await expect(controller.me(currentUser)).resolves.toEqual(userProfile);
    expect(syncAuthenticatedUser.execute).toHaveBeenCalledWith(currentUser);
  });

  it('delegates the users list endpoint to the list use case', async () => {
    const listUsers = {
      execute: jest.fn().mockResolvedValue([userProfile])
    } as unknown as ListUsersUseCase;
    const syncAuthenticatedUser = {
      execute: jest.fn()
    } as unknown as SyncAuthenticatedUserUseCase;
    const controller = new UsersController(
      listUsers,
      syncAuthenticatedUser,
      { execute: jest.fn() } as unknown as UpdateMyProfileImageUseCase,
      { execute: jest.fn() } as unknown as DeactivateUserUseCase
    );

    const currentUser = {
      sub: 'admin-sub',
      groups: []
    };

    await expect(controller.list(currentUser)).resolves.toEqual([userProfile]);
    expect(listUsers.execute).toHaveBeenCalledWith(currentUser);
  });

  it('delegates the user deactivation endpoint to the deactivate use case', async () => {
    const listUsers = {
      execute: jest.fn()
    } as unknown as ListUsersUseCase;
    const syncAuthenticatedUser = {
      execute: jest.fn()
    } as unknown as SyncAuthenticatedUserUseCase;
    const deactivateUser = {
      execute: jest.fn().mockResolvedValue({
        ...userProfile,
        status: 'INACTIVE'
      })
    } as unknown as DeactivateUserUseCase;
    const controller = new UsersController(
      listUsers,
      syncAuthenticatedUser,
      { execute: jest.fn() } as unknown as UpdateMyProfileImageUseCase,
      deactivateUser
    );
    const currentUser = {
      sub: 'admin-sub',
      groups: ['admin']
    };

    await expect(controller.deactivate(currentUser, 'user-id')).resolves.toEqual({
      ...userProfile,
      status: 'INACTIVE'
    });
    expect(deactivateUser.execute).toHaveBeenCalledWith(currentUser, 'user-id');
  });
});
