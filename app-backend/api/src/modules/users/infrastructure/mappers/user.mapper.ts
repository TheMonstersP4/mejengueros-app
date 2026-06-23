import { UserEntity } from '../../domain/entities/user.entity';

interface IUserPersistenceRecord {
  id: string;
  email: string;
  name?: string | null;
  pictureUrl?: string | null;
  identities?: IUserIdentityPersistenceRecord[];
}

interface IUserIdentityPersistenceRecord {
  provider: string;
  providerSubject: string;
}

/**
 * Maps Prisma user records to domain entities.
 */
export class UserMapper {
  /**
   * Converts a Prisma user model into a user entity.
   *
   * @param user - User record returned by Prisma.
   * @param currentIdentity - Identity used by the current request.
   * @returns User entity used by the application layer.
   */
  static toDomain(
    user: IUserPersistenceRecord,
    currentIdentity?: IUserIdentityPersistenceRecord | null
  ): UserEntity {
    const identity = currentIdentity ?? user.identities?.[0] ?? null;

    return UserEntity.fromPersistence({
      id: user.id,
      email: user.email,
      name: user.name,
      pictureUrl: user.pictureUrl,
      currentIdentity: identity
    });
  }
}
