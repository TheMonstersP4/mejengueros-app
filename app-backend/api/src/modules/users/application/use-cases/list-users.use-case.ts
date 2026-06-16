import { Inject, Injectable } from '@nestjs/common';
import type { IUserRepository } from '../../domain/repositories/user.repository';
import { USER_REPOSITORY } from '../../domain/repositories/user.repository';
import type { IUserProfileOutput } from '../dto/user-profile.output';

/**
 * Lists user profiles stored by the application.
 */
@Injectable()
export class ListUsersUseCase {
  constructor(
    @Inject(USER_REPOSITORY)
    private readonly userRepository: IUserRepository
  ) {}

  /**
   * Returns synchronized local user profiles.
   *
   * @returns User profiles ordered by recent activity.
   */
  async execute(): Promise<IUserProfileOutput[]> {
    const users = await this.userRepository.list();

    return users.map((user) => user.toProfile());
  }
}
