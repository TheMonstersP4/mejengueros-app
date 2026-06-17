import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

/**
 * HTTP response body for the authenticated user profile.
 */
export class UserProfileResponse {
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

  /**
   * Primary user email.
   */
  @ApiProperty({ example: 'player@example.com' })
  email!: string;

  /**
   * Display name when available.
   */
  @ApiPropertyOptional({ example: 'David Gutierrez' })
  name?: string;

  /**
   * Profile image URL when available.
   */
  @ApiPropertyOptional({ example: 'https://example.com/profile.jpg' })
  pictureUrl?: string;

  /**
   * Upstream identity provider name.
   */
  @ApiPropertyOptional({ example: 'Google' })
  provider?: string;
}
