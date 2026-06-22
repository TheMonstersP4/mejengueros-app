import { Inject, Injectable } from '@nestjs/common';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import type { UserEntity } from '../../domain/entities/user.entity';
import type {
  IExternalUserIdentity,
  IUserRepository
} from '../../domain/repositories/user.repository';
import { UserMapper } from '../mappers/user.mapper';
import {
  reconcileDemoOwnerRole,
  upsertAuthenticatedUserIdentity
} from '../provisioning/demo-owner-role-provisioning';

/**
 * Prisma-backed implementation of the user repository port.
 *
 * @remarks
 * Prisma models stay inside infrastructure. The repository maps persistence
 * records into domain entities before returning data to application code.
 */
@Injectable()
export class PrismaUserRepository implements IUserRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: PrismaService
  ) {}

  /**
   * Creates or updates a local user profile from verified Cognito claims.
   *
   * @param identity - Normalized external identity accepted by the users module.
   * @returns Synchronized user entity.
   */
  async syncAuthenticatedUser(identity: IExternalUserIdentity): Promise<UserEntity> {
    const user = await upsertAuthenticatedUserIdentity(this.prisma, identity);
    await reconcileDemoOwnerRole(this.prisma, user.id, identity);

    return UserMapper.toDomain(user);
  }

  /**
   * Finds a local user profile by Cognito subject.
   *
   * @param cognitoSub - Stable Cognito subject.
   * @returns User entity or `null` when no local user exists.
   */
  async findByCognitoSub(cognitoSub: string): Promise<UserEntity | null> {
    const user = await this.prisma.user.findUnique({
      where: { cognitoSub }
    });

    return user ? UserMapper.toDomain(user) : null;
  }

  /**
   * Lists local user profiles by most recently updated first.
   *
   * @returns User entities stored by the application.
   */
  async list(): Promise<UserEntity[]> {
    const users = await this.prisma.user.findMany({
      orderBy: { updatedAt: 'desc' }
    });

    return users.map((user) => UserMapper.toDomain(user));
  }
}
