import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

/**
 * HTTP response body for the authenticated Cognito identity.
 */
export class AuthenticatedUserResponse {
  /**
   * Stable Cognito subject.
   */
  @ApiProperty({ example: '21dbf550-b071-7037-4dc2-169c7a4b4c28' })
  sub!: string;

  /**
   * Verified email claim when available.
   */
  @ApiPropertyOptional({ example: 'player@example.com' })
  email?: string;

  /**
   * Display name claim when available.
   */
  @ApiPropertyOptional({ example: 'David Gutierrez' })
  name?: string;

  /**
   * Profile image claim when available.
   */
  @ApiPropertyOptional({ example: 'https://example.com/profile.jpg' })
  pictureUrl?: string;

  /**
   * Upstream identity provider name.
   */
  @ApiPropertyOptional({ example: 'Google' })
  provider?: string;

  /**
   * Cognito groups assigned to the user.
   */
  @ApiProperty({ example: ['players'], type: [String] })
  groups!: string[];
}
