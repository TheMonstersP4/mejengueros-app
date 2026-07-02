import type { UserRoleKind } from '../../domain/entities/user.entity';

/**
 * User profile returned by users application use cases.
 */
export interface IUserProfileOutput {
  /**
   * Internal application user ID.
   */
  id: string;

  /**
   * Stable subject from the current identity when available.
   */
  cognitoSub?: string;

  /**
   * Primary user email.
   */
  email: string;

  /**
   * Display name when available.
   */
  name?: string;

  /**
   * Profile image URL when available.
   */
  pictureUrl?: string;

  /**
   * Upstream identity provider name.
   */
  provider?: string;

  /**
   * Application roles assigned to the user.
   */
  roles: UserRoleKind[];
}
