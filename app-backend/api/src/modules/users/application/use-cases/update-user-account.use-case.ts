import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import type { UserRoleKind } from '../../domain/entities/user.entity';
import { USER_ROLE_KINDS } from '../../domain/entities/user.entity';
import { AdminRoleRequiredError } from '../../domain/errors/admin-role-required.error';
import { InvalidUserRoleError } from '../../domain/errors/invalid-user-role.error';
import { UserNotFoundError } from '../../domain/errors/user-not-found.error';
import type { IUserRepository } from '../../domain/repositories/user.repository';
import { USER_REPOSITORY } from '../../domain/repositories/user.repository';
import type { IUserProfileOutput } from '../dto/user-profile.output';
import { UserProfileService } from '../services/user-profile.service';

const ADMIN_GROUP = 'admin';

export interface IUpdateUserAccountInput {
  userId: string;
  name?: string;
  role?: string;
}

/**
 * Updates administrator-editable user account fields.
 */
@Injectable()
export class UpdateUserAccountUseCase {
  constructor(
    @Inject(USER_REPOSITORY)
    private readonly userRepository: IUserRepository,
    @Inject(UserProfileService)
    private readonly userProfileService: UserProfileService
  ) {}

  async execute(
    identity: IAuthenticatedUserOutput,
    input: IUpdateUserAccountInput
  ): Promise<IUserProfileOutput> {
    await this.ensureAdmin(identity);

    const role = this.parseRole(input.role);
    const user = await this.userRepository.updateAccount({
      userId: input.userId,
      name: input.name,
      role
    });

    if (!user) {
      throw new UserNotFoundError(input.userId);
    }

    return this.userProfileService.render(user);
  }

  private async ensureAdmin(identity: IAuthenticatedUserOutput): Promise<void> {
    if (this.hasAdminGroup(identity)) {
      return;
    }

    const currentUser = await this.userRepository.findByCognitoSub(identity.sub);

    if (!currentUser?.toProfile().roles.includes('ADMIN')) {
      throw new AdminRoleRequiredError(identity.sub);
    }
  }

  private hasAdminGroup(identity: IAuthenticatedUserOutput): boolean {
    return identity.groups.some(
      (group) => group.trim().toLowerCase() === ADMIN_GROUP
    );
  }

  private parseRole(role: string | undefined): UserRoleKind | undefined {
    if (role === undefined) {
      return undefined;
    }

    if (!USER_ROLE_KINDS.includes(role as UserRoleKind)) {
      throw new InvalidUserRoleError(role);
    }

    return role as UserRoleKind;
  }
}
