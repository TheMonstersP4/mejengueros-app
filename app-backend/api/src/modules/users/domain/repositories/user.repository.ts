import type { UserEntity, UserRoleKind } from '../entities/user.entity';

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
   * Whether the upstream provider verified the email claim.
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
}

/**
 * Atomic custom profile image replacement command.
 */
export interface IReplaceUserProfileImageInput {
  userId: string;
  imageUploadId: string;
}

/**
 * Administrative account update command.
 */
export interface IUpdateUserAccountInput {
  userId: string;
  name?: string;
  role?: UserRoleKind;
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
   * Atomically replaces the custom profile image relation for one user.
   *
   * @param input - Local user and confirmed image upload identifiers.
   * @returns Updated user entity.
   */
  replaceProfileImage(input: IReplaceUserProfileImageInput): Promise<UserEntity>;

  /**
   * Updates administrator-editable account fields for one user.
   *
   * @param input - Target user and allowed account fields.
   * @returns Updated user entity, or `null` when the target user does not exist.
   */
  updateAccount(input: IUpdateUserAccountInput): Promise<UserEntity | null>;

  /**
   * Lists local user profiles by most recently updated first.
   *
   * @returns User entities stored by the application.
   */
  list(): Promise<UserEntity[]>;

  /**
   * Marks one local user profile inactive without deleting related records.
   *
   * @param userId - Local user identifier.
   * @returns Updated user entity or `null` when no local user exists.
   */
  deactivateById(userId: string): Promise<UserEntity | null>;
}

/**
 * Dependency injection token for the user repository port.
 */
export const USER_REPOSITORY = Symbol('USER_REPOSITORY');
