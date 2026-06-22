/**
 * Verified identity claims attached to authenticated requests.
 */
export interface IAuthenticatedUserOutput {
  /**
   * Stable Cognito subject.
   */
  sub: string;

  /**
   * Verified email claim when available.
   */
  email?: string;

  /**
   * Whether Cognito marked the email claim as verified.
   */
  emailVerified?: boolean;

  /**
   * Display name claim when available.
   */
  name?: string;

  /**
   * Profile image claim when available.
   */
  pictureUrl?: string;

  /**
   * Upstream identity provider name.
   */
  provider?: string;

  /**
   * Cognito groups assigned to the user.
   */
  groups: string[];
}
