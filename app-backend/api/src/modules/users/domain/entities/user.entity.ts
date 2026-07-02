/**
 * Application-level role kinds available on a user profile.
 */
export type UserRoleKind = 'PLAYER' | 'OWNER' | 'ADMIN';

/**
 * Properties required to rebuild a user entity from persistence.
 */
export interface IUserEntityProps {
  /**
   * Internal application user ID.
   */
  id: string;

  /**
   * Primary user email stored by the application.
   */
  email: string;

  /**
   * Display name received from the identity provider.
   */
  name?: string | null;

  /**
   * Profile image URL received from the identity provider.
   */
  pictureUrl?: string | null;

  /**
   * Identity used for the current auth context.
   */
  currentIdentity?: IUserEntityIdentityProps | null;

  /**
   * Application roles assigned to the user.
   */
  roles?: UserRoleKind[] | null;
}

/**
 * Login identity linked to a local user profile.
 */
export interface IUserEntityIdentityProps {
  /**
   * Upstream identity provider name.
   */
  provider: string;

  /**
   * Provider-specific stable subject.
   */
  providerSubject: string;
}

/**
 * User profile snapshot exposed by the domain entity.
 */
export interface IUserProfileSnapshot {
  /**
   * Internal application user ID.
   */
  id: string;

  /**
   * Stable subject from the current identity.
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
   * Upstream identity provider name for the current identity.
   */
  provider?: string;

  /**
   * Application roles assigned to the user.
   */
  roles: UserRoleKind[];
}

/**
 * User aggregate used by application use cases.
 *
 * @remarks
 * Authentication is owned by Cognito. This entity represents the local
 * application profile that can be extended with business-specific user data.
 */
export class UserEntity {
  private constructor(private readonly props: IUserEntityProps) {}

  /**
   * Rebuilds a user entity from a trusted persistence record.
   *
   * @param props - User properties mapped from the database.
   * @returns User entity ready for application use.
   */
  static fromPersistence(props: IUserEntityProps): UserEntity {
    return new UserEntity(props);
  }

  /**
   * Converts the entity into the profile shape returned by use cases.
   *
   * @returns User profile data safe for API responses.
   */
  toProfile(): IUserProfileSnapshot {
    return {
      id: this.props.id,
      cognitoSub: this.props.currentIdentity?.providerSubject,
      email: this.props.email,
      name: this.props.name ?? undefined,
      pictureUrl: this.props.pictureUrl ?? undefined,
      provider: this.props.currentIdentity?.provider,
      roles: this.props.roles ?? []
    };
  }
}
