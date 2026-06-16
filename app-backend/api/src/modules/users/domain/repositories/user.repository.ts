import type { UserEntity } from '../entities/user.entity';

/**
 * External identity data accepted by the users module.
 *
 * @remarks
 * The auth module verifies the token. The users module only receives the
 * normalized identity claims it needs to sync the local profile.
 */
export interface IExternalUserIdentity {
  /**
   * Stable Cognito subject.
   */
  cognitoSub: string;

  /**
   * Verified email claim when available.
   */
  email?: string;

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
}

/**
 * Persistence contract for user profiles.
 */
export interface IUserRepository {
  /**
   * Creates or updates the local user profile from a verified external identity.
   *
   * @param identity - Normalized identity claims from the auth boundary.
   * @returns Synchronized user entity.
   */
  syncAuthenticatedUser(identity: IExternalUserIdentity): Promise<UserEntity>;

  /**
   * Finds a local user by Cognito subject.
   *
   * @param cognitoSub - Stable Cognito subject.
   * @returns Matching user entity or `null`.
   */
  findByCognitoSub(cognitoSub: string): Promise<UserEntity | null>;

  /**
   * Lists local user profiles by most recently updated first.
   *
   * @returns User entities stored by the application.
   */
  list(): Promise<UserEntity[]>;
}

/**
 * Dependency injection token for the user repository port.
 */
export const USER_REPOSITORY = Symbol('USER_REPOSITORY');
