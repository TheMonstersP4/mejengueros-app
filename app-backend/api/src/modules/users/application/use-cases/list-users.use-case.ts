import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import { AdminRoleRequiredError } from '../../domain/errors/admin-role-required.error';
import type { IUserRepository } from '../../domain/repositories/user.repository';
import { USER_REPOSITORY } from '../../domain/repositories/user.repository';
import type { IUserProfileOutput } from '../dto/user-profile.output';
import { UserProfileService } from '../services/user-profile.service';

const ADMIN_GROUP = 'admin';

/**
 * Lists user profiles stored by the application.
 */
@Injectable()
export class ListUsersUseCase {
  constructor(
    @Inject(USER_REPOSITORY)
    private readonly userRepository: IUserRepository,
    @Inject(UserProfileService)
    private readonly userProfileService: UserProfileService
  ) {}

  /**
   * Returns synchronized local user profiles.
   *
   * @returns User profiles ordered by recent activity.
   */
  async execute(identity: IAuthenticatedUserOutput): Promise<IUserProfileOutput[]> {
    if (!this.hasAdminGroup(identity)) {
      const currentUser = await this.userRepository.findByCognitoSub(identity.sub);

      if (!currentUser?.toProfile().roles.includes('ADMIN')) {
        throw new AdminRoleRequiredError(identity.sub);
      }
    }

    const users = await this.userRepository.list();

    return Promise.all(users.map((user) => this.userProfileService.render(user)));
  }

  private hasAdminGroup(identity: IAuthenticatedUserOutput): boolean {
    return identity.groups.some(
      (group) => group.trim().toLowerCase() === ADMIN_GROUP
    );
  }
}
