import { Controller, Get, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import type { IAuthenticatedUserOutput } from '../../../application/dto/authenticated-user.output';
import { CognitoAuthGuard } from '../guards/cognito-auth.guard';
import { CurrentUser } from '../../../../../shared/interfaces/http/decorators/current-user.decorator';

/**
 * HTTP endpoints for authentication-related current user data.
 */
@ApiTags('auth')
@ApiBearerAuth()
@Controller('auth')
export class AuthController {
  /**
   * Returns the authenticated Cognito identity attached to the request.
   *
   * @param user - Current authenticated user.
   * @returns Authenticated user claims.
   */
  @Get('me')
  @UseGuards(CognitoAuthGuard)
  @ApiOperation({ summary: 'Return the authenticated Cognito identity.' })
  me(@CurrentUser() user: IAuthenticatedUserOutput): IAuthenticatedUserOutput {
    return user;
  }
}
