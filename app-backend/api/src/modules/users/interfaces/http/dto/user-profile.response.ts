import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { UserIdentityResponse } from '../../../../../shared/interfaces/http/dto/user-identity.response';
import type { UserRoleKind } from '../../../domain/entities/user.entity';

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
   * Stable subject from the current identity when available.
   */
  @ApiPropertyOptional({ example: '21dbf550-b071-7037-4dc2-169c7a4b4c28' })
  cognitoSub?: string;

  /**
   * Application roles assigned to the user.
   */
  @ApiProperty({
    example: ['PLAYER'],
    enum: ['PLAYER', 'OWNER', 'ADMIN'],
    isArray: true
  })
  roles!: UserRoleKind[];
}
