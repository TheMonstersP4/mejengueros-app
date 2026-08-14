import {
  UserEntity,
  type UserRoleKind
} from '@/modules/users/domain/entities/user.entity';
import type { IUserRepository } from '@/modules/users/domain/repositories/user.repository';

interface ActiveUserRepositoryOptions {
  id?: string;
  subject?: string;
  email?: string;
  name?: string;
  pictureUrl?: string;
  provider?: string;
  roles?: UserRoleKind[];
}

export function createActiveUserRepository(
  options: ActiveUserRepositoryOptions = {}
): jest.Mocked<IUserRepository> {
  return {
    syncAuthenticatedUser: jest.fn(),
    findByCognitoSub: jest.fn().mockResolvedValue(createActiveUser(options)),
    replaceProfileImage: jest.fn(),
    updateAccount: jest.fn(),
    list: jest.fn(),
    deactivateById: jest.fn()
  };
}

export function createActiveUser(
  options: ActiveUserRepositoryOptions = {}
): UserEntity {
  const subject = options.subject ?? 'cognito-sub';

  return UserEntity.fromPersistence({
    id: options.id ?? 'user-id',
    email: options.email ?? 'user@example.test',
    name: options.name,
    pictureUrl: options.pictureUrl,
    status: 'ACTIVE',
    currentIdentity: {
      provider: options.provider ?? 'Google',
      providerSubject: subject
    },
    roles: options.roles ?? ['PLAYER']
  });
}
