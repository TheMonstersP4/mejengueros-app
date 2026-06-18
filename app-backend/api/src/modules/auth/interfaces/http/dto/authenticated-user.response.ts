import { ApiProperty } from '@nestjs/swagger';
import { UserIdentityResponse } from '../../../../../shared/interfaces/http/dto/user-identity.response';

/**
 * HTTP response body for the authenticated Cognito identity.
 */
export class AuthenticatedUserResponse extends UserIdentityResponse {
  /**
   * Stable Cognito subject.
   */
  @ApiProperty({ example: '21dbf550-b071-7037-4dc2-169c7a4b4c28' })
  sub!: string;

  /**
   * Cognito groups assigned to the user.
   */
  @ApiProperty({ example: ['players'], type: [String] })
  groups!: string[];
}
