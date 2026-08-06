import { Body, Controller, Get, Inject, Param, Patch, Put, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiTags } from '@nestjs/swagger';
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
import { UpdateMyProfileImageUseCase } from '../../../application/use-cases/update-my-profile-image.use-case';
import { UpdateUserAccountUseCase } from '../../../application/use-cases/update-user-account.use-case';
import { UserProfileResponse } from '../dto/user-profile.response';
// eslint-disable-next-line @typescript-eslint/consistent-type-imports -- Nest needs DTO classes at runtime for validation metadata.
import { UpdateProfileImageRequest } from '../dto/update-profile-image.request';
// eslint-disable-next-line @typescript-eslint/consistent-type-imports -- Nest needs DTO classes at runtime for validation metadata.
import { UpdateUserAccountRequest } from '../dto/update-user-account.request';

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
    private readonly syncAuthenticatedUser: SyncAuthenticatedUserUseCase,
    @Inject(UpdateMyProfileImageUseCase)
    private readonly updateMyProfileImage: UpdateMyProfileImageUseCase,
    @Inject(UpdateUserAccountUseCase)
    private readonly updateUserAccount: UpdateUserAccountUseCase
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
  @ApiEnvelopeErrors(401, 403, 503)
  async list(
    @CurrentUser() user: IAuthenticatedUserOutput
  ): Promise<UserProfileResponse[]> {
    return this.listUsers.execute(user);
  }

  /**
   * Updates administrator-editable account fields for one user.
   */
  @Patch(':userId')
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'Update one user account as an administrator.',
    description:
      'Allows administrators to update only the account display name and application role.'
  })
  @ApiBody({ type: UpdateUserAccountRequest })
  @ApiEnvelopeOk(
    UserProfileResponse,
    'Updated user profile wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401, 403, 404)
  async update(
    @CurrentUser() user: IAuthenticatedUserOutput,
    @Param('userId') userId: string,
    @Body() request: UpdateUserAccountRequest
  ): Promise<UserProfileResponse> {
    return this.updateUserAccount.execute(user, {
      userId,
      name: request.name,
      role: request.role
    });
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

  /**
   * Associates a confirmed upload as the authenticated user's custom profile image.
   */
  @Put('me/profile-image')
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({
    summary: 'Replace the current user custom profile image.',
    description:
      'Validates ownership and purpose of a confirmed upload, stores the durable association, and returns the effective profile with a fresh read URL.'
  })
  @ApiBody({ type: UpdateProfileImageRequest })
  @ApiEnvelopeOk(
    UserProfileResponse,
    'Updated current user profile wrapped in the API response envelope.'
  )
  @ApiEnvelopeErrors(400, 401, 403, 404, 409, 502)
  async updateProfileImage(
    @CurrentUser() user: IAuthenticatedUserOutput,
    @Body() request: UpdateProfileImageRequest
  ): Promise<UserProfileResponse> {
    return this.updateMyProfileImage.execute(user, request.imageUploadId);
  }
}
