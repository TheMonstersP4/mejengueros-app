import { Controller, Get, Inject, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import type { IAuthenticatedUserOutput } from '../../../../auth/application/dto/authenticated-user.output';
import { CognitoAuthGuard } from '../../../../auth/interfaces/http/guards/cognito-auth.guard';
import { CurrentUser } from '../../../../../shared/interfaces/http/decorators/current-user.decorator';
import { ListUsersUseCase } from '../../../application/use-cases/list-users.use-case';
import { SyncAuthenticatedUserUseCase } from '../../../application/use-cases/sync-authenticated-user.use-case';
import type { UserProfileResponse } from '../dto/user-profile.response';

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
  @ApiOperation({ summary: 'List synchronized application users.' })
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
  @ApiOperation({ summary: 'Synchronize and return the current user profile.' })
  async me(
    @CurrentUser() user: IAuthenticatedUserOutput
  ): Promise<UserProfileResponse> {
    return this.syncAuthenticatedUser.execute(user);
  }
}
