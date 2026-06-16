/**
 * User profile returned by users application use cases.
 */
export interface IUserProfileOutput {
  /**
   * Internal application user ID.
   */
  id: string;

  /**
   * Stable Cognito subject linked to this user.
   */
  cognitoSub: string;

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
}
