import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

/**
 * HTTP response body for the authenticated user profile.
 */
export class UserProfileResponse {
  /**
   * Internal application user ID.
   */
  @ApiProperty()
  id!: string;

  /**
   * Stable Cognito subject linked to this user.
   */
  @ApiProperty()
  cognitoSub!: string;

  /**
   * Primary user email.
   */
  @ApiProperty()
  email!: string;

  /**
   * Display name when available.
   */
  @ApiPropertyOptional()
  name?: string;

  /**
   * Profile image URL when available.
   */
  @ApiPropertyOptional()
  pictureUrl?: string;

  /**
   * Upstream identity provider name.
   */
  @ApiPropertyOptional()
  provider?: string;
}
