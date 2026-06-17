import { Controller, Get, Inject, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import {
  ApiEnvelopeArrayOk,
  ApiEnvelopeErrors,
  ApiEnvelopeOk
} from '../../../../../shared/interfaces/http/swagger/api-envelope.decorators';
import type { IAuthenticatedUserOutput } from '../../../../auth/application/dto/authenticated-user.output';
import { CognitoAuthGuard } from '../../../../auth/interfaces/http/guards/cognito-auth.guard';
import { CurrentUser } from '../../../../../shared/interfaces/http/decorators/current-user.decorator';
import { ListUsersUseCase } from '../../../application/use-cases/list-users.use-case';
import { SyncAuthenticatedUserUseCase } from '../../../application/use-cases/sync-authenticated-user.use-case';
import { UserProfileResponse } from '../dto/user-profile.response';

/**
 * HTTP endpoints for authenticated user profiles.
 */
@ApiTags('users')
@ApiBearerAuth()
@Controller('users')
export class UsersController {
  constructor(
    @Inject(ListUsersUseCase)
    private readonly listUsers: ListUsersUseCase,
    @Inject(SyncAuthenticatedUserUseCase)
    private readonly syncAuthenticatedUser: SyncAuthenticatedUserUseCase
  ) {}

  /**
   * Returns synchronized users stored by the application.
   *
   * @returns Local user profile responses.
   */
  @Get()
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'List synchronized application users.',
    description:
      'Returns user profiles already synchronized into the application database.'
  })
  @ApiEnvelopeArrayOk(
    UserProfileResponse,
    'Synchronized users wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(401)
  async list(): Promise<UserProfileResponse[]> {
    return this.listUsers.execute();
  }

  /**
   * Returns the local profile for the authenticated user.
   *
   * @param user - Current authenticated user.
   * @returns Local user profile response.
   */
  @Get('me')
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'Synchronize and return the current user profile.',
    description:
      'Uses the authenticated Cognito claims to create or update the local user profile, then returns the synchronized profile.'
  })
  @ApiEnvelopeOk(
    UserProfileResponse,
    'Current user profile wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(401, 502)
  async me(
    @CurrentUser() user: IAuthenticatedUserOutput
  ): Promise<UserProfileResponse> {
    return this.syncAuthenticatedUser.execute(user);
  }
}
