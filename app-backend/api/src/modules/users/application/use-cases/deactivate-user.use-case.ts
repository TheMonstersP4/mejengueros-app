import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import { AdminRoleRequiredError } from '../../domain/errors/admin-role-required.error';
import { UserNotFoundError } from '../../domain/errors/user-not-found.error';
import type { IUserRepository } from '../../domain/repositories/user.repository';
import { USER_REPOSITORY } from '../../domain/repositories/user.repository';
import type { IUserProfileOutput } from '../dto/user-profile.output';
import { UserProfileService } from '../services/user-profile.service';

const ADMIN_GROUP = 'admin';

/**
 * Deactivates local user accounts without removing historical data.
 */
@Injectable()
export class DeactivateUserUseCase {
  constructor(
    @Inject(USER_REPOSITORY)
    private readonly userRepository: IUserRepository,
    @Inject(UserProfileService)
    private readonly userProfileService: UserProfileService
  ) {}

  /**
   * Marks a local user account inactive when the actor is an administrator.
   */
  async execute(
    identity: IAuthenticatedUserOutput,
    userId: string
  ): Promise<IUserProfileOutput> {
    if (!this.hasAdminGroup(identity)) {
      const currentUser = await this.userRepository.findByCognitoSub(identity.sub);

      if (!currentUser?.toProfile().roles.includes('ADMIN')) {
        throw new AdminRoleRequiredError(identity.sub);
      }
    }

    const user = await this.userRepository.deactivateById(userId);

    if (!user) {
      throw new UserNotFoundError(userId);
    }

    return this.userProfileService.render(user);
  }

  private hasAdminGroup(identity: IAuthenticatedUserOutput): boolean {
    return identity.groups.some(
      (group) => group.trim().toLowerCase() === ADMIN_GROUP
    );
  }
}
