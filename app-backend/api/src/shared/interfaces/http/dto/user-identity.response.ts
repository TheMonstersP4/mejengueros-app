import { ApiPropertyOptional } from '@nestjs/swagger';

/**
 * Shared identity claims exposed by user-facing API responses.
 */
export class UserIdentityResponse {
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
   * Profile image URL when available.
   */
  @ApiPropertyOptional({ example: 'https://example.com/profile.jpg' })
  pictureUrl?: string;

  /**
   * Upstream identity provider name when available.
   */
  @ApiPropertyOptional({ example: 'Google' })
  provider?: string;
}
