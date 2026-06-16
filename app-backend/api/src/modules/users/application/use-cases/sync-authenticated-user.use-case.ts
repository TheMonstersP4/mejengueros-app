import { Inject, Injectable } from '@nestjs/common';
import type { IAuthenticatedUserOutput } from '../../../auth/application/dto/authenticated-user.output';
import type { IUserRepository } from '../../domain/repositories/user.repository';
import { USER_REPOSITORY } from '../../domain/repositories/user.repository';
import type { IUserProfileOutput } from '../dto/user-profile.output';

/**
 * Synchronizes Cognito identity claims with the local application user record.
 *
 * @remarks
 * Cognito owns authentication. PostgreSQL stores application-specific user
 * profile data.
 */
@Injectable()
export class SyncAuthenticatedUserUseCase {
  constructor(
    @Inject(USER_REPOSITORY)
    private readonly userRepository: IUserRepository
  ) {}

  /**
   * Synchronizes a verified identity with the local user profile.
   *
   * @param identity - Authenticated identity attached by the auth guard.
   * @returns Local user profile.
   */
  async execute(identity: IAuthenticatedUserOutput): Promise<IUserProfileOutput> {
    const user = await this.userRepository.syncAuthenticatedUser({
      cognitoSub: identity.sub,
      email: identity.email,
      name: identity.name,
      pictureUrl: identity.pictureUrl,
      provider: identity.provider
    });

    return user.toProfile();
  }
}
