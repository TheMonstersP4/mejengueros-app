import { ApiProperty } from '@nestjs/swagger';
import { UserIdentityResponse } from '../../../../../shared/interfaces/http/dto/user-identity.response';

/**
 * HTTP response body for the authenticated user profile.
 */
export class UserProfileResponse extends UserIdentityResponse {
  /**
   * Internal application user ID.
   */
  @ApiProperty({ example: '01J0W5K4T2S8Q9B7J6V4M3N2P1' })
  id!: string;

  /**
   * Stable Cognito subject linked to this user.
   */
  @ApiProperty({ example: '21dbf550-b071-7037-4dc2-169c7a4b4c28' })
  cognitoSub!: string;

}
