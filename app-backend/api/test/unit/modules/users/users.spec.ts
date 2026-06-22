import { ListUsersUseCase } from '@/modules/users/application/use-cases/list-users.use-case';
import { SyncAuthenticatedUserUseCase } from '@/modules/users/application/use-cases/sync-authenticated-user.use-case';
import { UserEntity } from '@/modules/users/domain/entities/user.entity';
import type { IUserRepository } from '@/modules/users/domain/repositories/user.repository';
import { UserMapper } from '@/modules/users/infrastructure/mappers/user.mapper';
import { PrismaUserRepository } from '@/modules/users/infrastructure/persistence/prisma-user.repository';
import { UsersController } from '@/modules/users/interfaces/http/controllers/users.controller';

describe('users module behavior', () => {
  const persistenceUser = {
    id: 'user-id',
    cognitoSub: 'cognito-sub',
    email: 'user@example.test',
    name: 'User Name',
    pictureUrl: 'https://example.test/avatar.png',
    provider: 'Google'
  };

  it('converts user entities to API-safe profiles', () => {
    const entity = UserEntity.fromPersistence({
      ...persistenceUser,
      name: null,
      pictureUrl: null,
      provider: null
    });

    expect(entity.toProfile()).toEqual({
      id: 'user-id',
      cognitoSub: 'cognito-sub',
      email: 'user@example.test',
      name: undefined,
      pictureUrl: undefined,
      provider: undefined
    });
  });

  it('maps Prisma users to domain entities', () => {
    expect(UserMapper.toDomain(persistenceUser as never).toProfile()).toEqual(
      persistenceUser
    );
  });

  it('syncs authenticated users through the repository port', async () => {
    const entity = UserEntity.fromPersistence(persistenceUser);
    const repository = {
      syncAuthenticatedUser: jest.fn().mockResolvedValue(entity),
      findByCognitoSub: jest.fn(),
      list: jest.fn()
    } satisfies IUserRepository;
    const useCase = new SyncAuthenticatedUserUseCase(repository);

    await expect(
      useCase.execute({
        sub: 'cognito-sub',
        email: 'user@example.test',
        name: 'User Name',
        pictureUrl: 'https://example.test/avatar.png',
        provider: 'Google',
        groups: ['admin']
      })
    ).resolves.toEqual(persistenceUser);
    expect(repository.syncAuthenticatedUser).toHaveBeenCalledWith({
      cognitoSub: 'cognito-sub',
      email: 'user@example.test',
      name: 'User Name',
      pictureUrl: 'https://example.test/avatar.png',
      provider: 'Google'
    });
  });

  it('upserts authenticated users and maps the result', async () => {
    const originalDemoOwnerEmails = process.env.DEMO_OWNER_EMAILS;
    const originalDemoOwnerSubs = process.env.DEMO_OWNER_SUBS;
    delete process.env.DEMO_OWNER_EMAILS;
    delete process.env.DEMO_OWNER_SUBS;
    const prisma = {
      user: {
        upsert: jest.fn().mockResolvedValue(persistenceUser),
        findUnique: jest.fn(),
        findMany: jest.fn()
      },
      userRole: {
        upsert: jest.fn(),
        deleteMany: jest.fn().mockResolvedValue({ count: 0 })
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(
      repository.syncAuthenticatedUser({
        cognitoSub: 'cognito-sub',
        name: 'User Name'
      })
    ).resolves.toEqual(expect.any(UserEntity));

    expect(prisma.user.upsert).toHaveBeenCalledWith({
      where: { cognitoSub: 'cognito-sub' },
      create: {
        cognitoSub: 'cognito-sub',
        email: 'cognito-sub@unknown.local',
        name: 'User Name',
        pictureUrl: undefined,
        provider: undefined
      },
      update: {
        email: undefined,
        name: 'User Name',
        pictureUrl: undefined,
        provider: undefined
      }
    });
    expect(prisma.userRole.upsert).not.toHaveBeenCalled();
    expect(prisma.userRole.deleteMany).toHaveBeenCalledWith({
      where: {
        userId: 'user-id',
        role: 'OWNER'
      }
    });
    process.env.DEMO_OWNER_EMAILS = originalDemoOwnerEmails;
    process.env.DEMO_OWNER_SUBS = originalDemoOwnerSubs;
  });

  it('provisions the OWNER role during sync when the demo email allowlist matches', async () => {
    const originalDemoOwnerEmails = process.env.DEMO_OWNER_EMAILS;
    const originalDemoOwnerSubs = process.env.DEMO_OWNER_SUBS;
    process.env.DEMO_OWNER_EMAILS = 'owner@example.test,another@example.test';
    delete process.env.DEMO_OWNER_SUBS;
    const prisma = {
      user: {
        upsert: jest.fn().mockResolvedValue(persistenceUser),
        findUnique: jest.fn(),
        findMany: jest.fn()
      },
      userRole: {
        upsert: jest.fn().mockResolvedValue({ id: 'owner-role-id' }),
        deleteMany: jest.fn()
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await repository.syncAuthenticatedUser({
      cognitoSub: 'cognito-sub',
      email: 'OWNER@example.test',
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

  it('revokes the demo OWNER role during sync when the allowlist no longer matches', async () => {
    const originalDemoOwnerEmails = process.env.DEMO_OWNER_EMAILS;
    const originalDemoOwnerSubs = process.env.DEMO_OWNER_SUBS;
    delete process.env.DEMO_OWNER_EMAILS;
    delete process.env.DEMO_OWNER_SUBS;
    const prisma = {
      user: {
        upsert: jest.fn().mockResolvedValue(persistenceUser),
        findUnique: jest.fn(),
        findMany: jest.fn()
      },
      userRole: {
        upsert: jest.fn(),
        deleteMany: jest.fn().mockResolvedValue({ count: 1 })
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await repository.syncAuthenticatedUser({
      cognitoSub: 'cognito-sub',
      email: 'owner@example.test',
      name: 'User Name'
    });

    expect(prisma.userRole.upsert).not.toHaveBeenCalled();
    expect(prisma.userRole.deleteMany).toHaveBeenCalledWith({
      where: {
        userId: 'user-id',
        role: 'OWNER'
      }
    });
    process.env.DEMO_OWNER_EMAILS = originalDemoOwnerEmails;
    process.env.DEMO_OWNER_SUBS = originalDemoOwnerSubs;
  });

  it('finds users by Cognito subject', async () => {
    const prisma = {
      user: {
        upsert: jest.fn(),
        findUnique: jest.fn().mockResolvedValueOnce(persistenceUser),
        findMany: jest.fn()
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    const user = await repository.findByCognitoSub('cognito-sub');

    expect(user?.toProfile()).toEqual(persistenceUser);
    expect(prisma.user.findUnique).toHaveBeenCalledWith({
      where: { cognitoSub: 'cognito-sub' }
    });
  });

  it('returns null when a Cognito subject is unknown', async () => {
    const prisma = {
      user: {
        upsert: jest.fn(),
        findUnique: jest.fn().mockResolvedValueOnce(null),
        findMany: jest.fn()
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(repository.findByCognitoSub('missing-sub')).resolves.toBeNull();
  });

  it('lists synchronized users through the repository port', async () => {
    const entity = UserEntity.fromPersistence(persistenceUser);
    const repository = {
      syncAuthenticatedUser: jest.fn(),
      findByCognitoSub: jest.fn(),
      list: jest.fn().mockResolvedValue([entity])
    } satisfies IUserRepository;
    const useCase = new ListUsersUseCase(repository);

    await expect(useCase.execute()).resolves.toEqual([persistenceUser]);
    expect(repository.list).toHaveBeenCalledTimes(1);
  });

  it('lists users from Prisma by recent updates', async () => {
    const prisma = {
      user: {
        upsert: jest.fn(),
        findUnique: jest.fn(),
        findMany: jest.fn().mockResolvedValueOnce([persistenceUser])
      }
    };
    const repository = new PrismaUserRepository(prisma as never);

    await expect(repository.list()).resolves.toHaveLength(1);
    expect(prisma.user.findMany).toHaveBeenCalledWith({
      orderBy: { updatedAt: 'desc' }
    });
  });

  it('delegates the current user endpoint to the sync use case', async () => {
    const listUsers = {
      execute: jest.fn()
    } as unknown as ListUsersUseCase;
    const syncAuthenticatedUser = {
      execute: jest.fn().mockResolvedValue(persistenceUser)
    } as unknown as SyncAuthenticatedUserUseCase;
    const controller = new UsersController(listUsers, syncAuthenticatedUser);
    const currentUser = {
      sub: 'cognito-sub',
      groups: []
    };

    await expect(controller.me(currentUser)).resolves.toEqual(persistenceUser);
    expect(syncAuthenticatedUser.execute).toHaveBeenCalledWith(currentUser);
  });

  it('delegates the users list endpoint to the list use case', async () => {
    const listUsers = {
      execute: jest.fn().mockResolvedValue([persistenceUser])
    } as unknown as ListUsersUseCase;
    const syncAuthenticatedUser = {
      execute: jest.fn()
    } as unknown as SyncAuthenticatedUserUseCase;
    const controller = new UsersController(listUsers, syncAuthenticatedUser);

    await expect(controller.list()).resolves.toEqual([persistenceUser]);
    expect(listUsers.execute).toHaveBeenCalledTimes(1);
  });
});
