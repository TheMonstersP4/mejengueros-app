import { Inject, Injectable } from '@nestjs/common';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import type { UserEntity } from '../../domain/entities/user.entity';
import type {
  IExternalUserIdentity,
  IReplaceUserProfileImageInput,
  IUpdateUserAccountInput,
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
   * Updates administrator-editable account fields for one user.
   */
  async updateAccount(input: IUpdateUserAccountInput): Promise<UserEntity | null> {
    const include = {
      identities: true,
      roles: true
    } as const;

    if (input.role) {
      const role = input.role;
      const user = await this.prisma.$transaction(async (tx) => {
        const existingUser = await tx.user.findUnique({
          where: { id: input.userId },
          include
        });

        if (!existingUser) {
          return null;
        }

        await tx.userRole.deleteMany({ where: { userId: input.userId } });
        await tx.userRole.create({
          data: {
            userId: input.userId,
            role
          }
        });

        return tx.user.update({
          where: { id: input.userId },
          data: buildAccountUpdateData(input),
          include
        });
      });

      return user ? UserMapper.toDomain(user) : null;
    }

    const existingUser = await this.prisma.user.findUnique({
      where: { id: input.userId },
      include
    });

    if (!existingUser) {
      return null;
    }

    if (input.name === undefined) {
      return UserMapper.toDomain(existingUser);
    }

    const user = await this.prisma.user.update({
      where: { id: input.userId },
      data: buildAccountUpdateData(input),
      include
    });

    return UserMapper.toDomain(user);
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

  /**
   * Marks one local user profile inactive without deleting relationships.
   */
  async deactivateById(userId: string): Promise<UserEntity | null> {
    const existingUser = await this.prisma.user.findUnique({
      where: { id: userId },
      include: {
        identities: true,
        roles: true
      }
    });

    if (!existingUser) {
      return null;
    }

    if (existingUser.status === 'INACTIVE') {
      return UserMapper.toDomain(existingUser);
    }

    const user = await this.prisma.user.update({
      where: { id: userId },
      data: { status: 'INACTIVE' },
      include: {
        identities: true,
        roles: true
      }
    });

    return UserMapper.toDomain(user);
  }
}

function buildAccountUpdateData(input: IUpdateUserAccountInput): { name?: string } {
  return input.name === undefined ? {} : { name: input.name };
}

function isPrismaUniqueConstraintError(error: unknown): boolean {
  return (
    typeof error === 'object' &&
    error !== null &&
    'code' in error &&
    error.code === 'P2002'
  );
}
