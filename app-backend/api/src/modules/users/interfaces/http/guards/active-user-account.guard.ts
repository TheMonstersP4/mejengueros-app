import type { CanActivate, ExecutionContext } from '@nestjs/common';
import { Inject, Injectable } from '@nestjs/common';
import type { FastifyRequest } from 'fastify';
import type { IAuthenticatedUserOutput } from '../../../../auth/application/dto/authenticated-user.output';
import { UserAccountInactiveError } from '../../../domain/errors/user-account-inactive.error';
import type { IUserRepository } from '../../../domain/repositories/user.repository';
import { USER_REPOSITORY } from '../../../domain/repositories/user.repository';

/**
 * Blocks operational flows for local accounts that were deactivated.
 */
@Injectable()
export class ActiveUserAccountGuard implements CanActivate {
  constructor(
    @Inject(USER_REPOSITORY)
    private readonly userRepository: IUserRepository
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<
      FastifyRequest & { user?: IAuthenticatedUserOutput }
    >();
    const cognitoSub = request.user?.sub;

    if (!cognitoSub) {
      return true;
    }

    const localUser = await this.userRepository.findByCognitoSub(cognitoSub);
    const profile = localUser?.toProfile();

    if (profile?.status === 'INACTIVE') {
      throw new UserAccountInactiveError(cognitoSub, profile.id);
    }

    return true;
  }
}
