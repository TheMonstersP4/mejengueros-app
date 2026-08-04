import { Inject, Injectable } from '@nestjs/common';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import type { UserEntity } from '../../domain/entities/user.entity';
import type {
  IExternalUserIdentity,
  IReplaceUserProfileImageInput,
  IUserRepository
} from '../../domain/repositories/user.repository';
import { InvalidProfileImageUploadError } from '../../domain/errors/invalid-profile-image-upload.error';
import { UserMapper } from '../mappers/user.mapper';
import { UserListUnavailableError } from '../errors/user-list-unavailable.error';
import {
  grantDemoOwnerRoleIfEligible,
  type IUserPersistenceRecord,
  upsertAuthenticatedUserIdentity
} from '../provisioning/demo-owner-role-provisioning';

const COGNITO_NATIVE_PROVIDER = 'Cognito';

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
    const user = await upsertAuthenticatedUserIdentity<IUserPersistenceRecord>(
      this.prisma,
      identity
    );
    await grantDemoOwnerRoleIfEligible(this.prisma, user.id, identity);

    const userRoles = await this.prisma.userRole.findMany({
      where: { userId: user.id },
      select: { role: true }
    });

    return UserMapper.toDomain(
      { ...user, roles: userRoles },
      {
        provider: identity.provider ?? COGNITO_NATIVE_PROVIDER,
        providerSubject: identity.cognitoSub
      }
    );
  }

  /**
   * Finds a local user profile by Cognito subject.
   *
   * @param cognitoSub - Stable Cognito subject.
   * @returns User entity or `null` when no local user exists.
   */
  async findByCognitoSub(cognitoSub: string): Promise<UserEntity | null> {
    try {
      const identity = await this.prisma.userIdentity.findFirst({
        where: { providerSubject: cognitoSub },
        include: {
          user: {
            include: {
              identities: true,
              roles: true
            }
          }
        }
      });

      return identity ? UserMapper.toDomain(identity.user, identity) : null;
    } catch (error) {
      throw new UserListUnavailableError(error);
    }
  }

  /**
   * Atomically replaces one user's custom profile image relation.
   */
  async replaceProfileImage(
    input: IReplaceUserProfileImageInput
  ): Promise<UserEntity> {
    try {
      const user = await this.prisma.user.update({
        where: { id: input.userId },
        data: { profileImageUploadId: input.imageUploadId },
        include: {
          identities: true,
          roles: true
        }
      });

      return UserMapper.toDomain(user);
    } catch (error) {
      if (isPrismaUniqueConstraintError(error)) {
        throw InvalidProfileImageUploadError.alreadyAssigned(
          input.imageUploadId
        );
      }

      throw error;
    }
  }

  /**
   * Lists local user profiles by most recently updated first.
   *
   * @returns User entities stored by the application.
   */
  async list(): Promise<UserEntity[]> {
    let users: Awaited<ReturnType<typeof this.prisma.user.findMany>>;

    try {
      users = await this.prisma.user.findMany({
        include: {
          identities: true,
          roles: true
        },
        orderBy: { updatedAt: 'desc' }
      });
    } catch (error) {
      throw new UserListUnavailableError(error);
    }

    return users.map((user) => UserMapper.toDomain(user));
  }
}

function isPrismaUniqueConstraintError(error: unknown): boolean {
  return (
    typeof error === 'object' &&
    error !== null &&
    'code' in error &&
    error.code === 'P2002'
  );
}
