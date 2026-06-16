import type { User } from '../../../../generated/prisma/client';
import { UserEntity } from '../../domain/entities/user.entity';

/**
 * Maps Prisma user records to domain entities.
 */
export class UserMapper {
  /**
   * Converts a Prisma user model into a user entity.
   *
   * @param user - User record returned by Prisma.
   * @returns User entity used by the application layer.
   */
  static toDomain(user: User): UserEntity {
    return UserEntity.fromPersistence({
      id: user.id,
      cognitoSub: user.cognitoSub,
      email: user.email,
      name: user.name,
      pictureUrl: user.pictureUrl,
      provider: user.provider
    });
  }
}
