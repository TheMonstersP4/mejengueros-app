import type { IExternalUserIdentity } from '../../domain/repositories/user.repository';
import { UserEmailAlreadyExistsError } from '../../domain/errors/user-email-already-exists.error';

const COGNITO_NATIVE_PROVIDER = 'Cognito';
const MAX_USER_IDENTITY_UPSERT_ATTEMPTS = 2;

export interface IUserPersistenceRecord {
  id: string;
  email: string;
  name?: string | null;
  pictureUrl?: string | null;
  identities?: IUserIdentityRecord[];
}

interface IUserIdentityRecord {
  provider: string;
  providerSubject: string;
}

interface IUserProfileUpdatePayload {
  email?: string;
  name?: string;
  pictureUrl?: string;
}

export interface IUserIdentityPersistenceClient<TResult extends { id: string }> {
  user: {
    findUnique(args: unknown): Promise<IUserPersistenceRecord | null>;
    update(args: unknown): Promise<TResult>;
    create(args: unknown): Promise<TResult>;
  };
  userIdentity: {
    findUnique(args: unknown): Promise<{
      userId: string;
      user?: IUserPersistenceRecord;
    } | null>;
  };
}

export interface IUserRoleUpsertClient {
  userRole: {
    upsert(args: {
      where: {
        userId_role: {
          userId: string;
          role: 'OWNER';
        };
      };
      create: {
        userId: string;
        role: 'OWNER';
      };
      update: Record<string, never>;
    }): Promise<unknown>;
  };
}

interface IUserRoleProvisioningClient extends IUserRoleUpsertClient {
  userRole: IUserRoleUpsertClient['userRole'] & {
    findUnique(args: {
      where: {
        userId_role: {
          userId: string;
          role: 'OWNER';
        };
      };
      select: { id: true };
    }): Promise<{ id: string } | null>;
  };
}

interface IDemoOwnerRoleProvisioningConfig {
  emails: Set<string>;
  cognitoSubs: Set<string>;
}

function parseCsvAllowlist(value: string | undefined, normalize: (value: string) => string): Set<string> {
  if (!value) {
    return new Set();
  }

  return new Set(
    value
      .split(',')
      .map((entry) => normalize(entry.trim()))
      .filter((entry) => entry.length > 0)
  );
}

function readDemoOwnerRoleProvisioningConfig(): IDemoOwnerRoleProvisioningConfig {
  return {
    emails: parseCsvAllowlist(process.env.DEMO_OWNER_EMAILS, (value) => value.toLowerCase()),
    cognitoSubs: parseCsvAllowlist(process.env.DEMO_OWNER_SUBS, (value) => value)
  };
}

export function shouldProvisionDemoOwnerRole(identity: IExternalUserIdentity): boolean {
  const config = readDemoOwnerRoleProvisioningConfig();
  const normalizedEmail = identity.email?.trim().toLowerCase();

  return (
    config.cognitoSubs.has(identity.cognitoSub) ||
    (identity.emailVerified === true &&
      normalizedEmail !== undefined &&
      config.emails.has(normalizedEmail))
  );
}

export async function upsertAuthenticatedUserIdentity<TResult extends { id: string }>(
  client: IUserIdentityPersistenceClient<TResult>,
  identity: IExternalUserIdentity,
  options?: { selectIdOnly?: boolean; retryOnUniqueConstraint?: boolean }
): Promise<TResult> {
  const readArgs = buildUserReadArgs(options);
  const identityLookup = buildIdentityLookup(identity);
  const shouldRetryOnUniqueConstraint = options?.retryOnUniqueConstraint !== false;
  const maxAttempts = shouldRetryOnUniqueConstraint ? MAX_USER_IDENTITY_UPSERT_ATTEMPTS : 1;
  let lastUniqueConstraintError: unknown;

  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    const existingIdentity = await findUserIdentity(client, identityLookup);

    if (existingIdentity) {
      return client.user.update({
        where: { id: existingIdentity.userId },
        data: buildUserProfileUpdatePayload(identity),
        ...readArgs
      });
    }

    if (identity.email) {
      const existingByEmail = await client.user.findUnique({
        where: { email: identity.email },
        include: {
          identities: true
        }
      });

      if (existingByEmail) {
        if (identity.emailVerified !== true) {
          throw new UserEmailAlreadyExistsError(
            identity.email,
            providersFromUser(existingByEmail)
          );
        }

        try {
          return await client.user.update({
            where: { id: existingByEmail.id },
            data: {
              ...buildUserProfileUpdatePayload(identity),
              identities: {
                create: buildUserIdentityCreatePayload(identity)
              }
            },
            ...readArgs
          });
        } catch (error) {
          if (!isPrismaUniqueConstraintError(error)) {
            throw error;
          }

          if (!shouldRetryOnUniqueConstraint) {
            throw error;
          }

          lastUniqueConstraintError = error;
          continue;
        }
      }
    }

    try {
      return await client.user.create({
        data: {
          ...buildUserCreatePayload(identity),
          identities: {
            create: buildUserIdentityCreatePayload(identity)
          }
        },
        ...readArgs
      });
    } catch (error) {
      if (!isPrismaUniqueConstraintError(error)) {
        throw error;
      }

      if (!shouldRetryOnUniqueConstraint) {
        throw error;
      }

      lastUniqueConstraintError = error;
    }
  }

  if (!shouldRetryOnUniqueConstraint) {
    throw lastUniqueConstraintError;
  }

  const existingIdentity = await findUserIdentity(client, identityLookup);

  if (existingIdentity) {
    return client.user.update({
      where: { id: existingIdentity.userId },
      data: buildUserProfileUpdatePayload(identity),
      ...readArgs
    });
  }

  throw lastUniqueConstraintError;
}

async function findUserIdentity<TResult extends { id: string }>(
  client: IUserIdentityPersistenceClient<TResult>,
  identityLookup: { provider: string; providerSubject: string }
): Promise<
  | {
      userId: string;
      user?: IUserPersistenceRecord;
    }
  | null
> {
  return client.userIdentity.findUnique({
    where: {
      provider_providerSubject: identityLookup
    },
    include: {
      user: {
        include: {
          identities: true
        }
      }
    }
  });
}

function buildUserCreatePayload(identity: IExternalUserIdentity): {
  email: string;
  name?: string;
  pictureUrl?: string;
} {
  return removeUndefinedValues({
    email: identity.email ?? `${identity.cognitoSub}@unknown.local`,
    name: identity.name,
    pictureUrl: identity.pictureUrl
  }) as {
    email: string;
    name?: string;
    pictureUrl?: string;
  };
}

function buildUserProfileUpdatePayload(
  identity: IExternalUserIdentity
): IUserProfileUpdatePayload {
  return removeUndefinedValues({
    email: identity.emailVerified === true ? identity.email : undefined,
    name: identity.name,
    pictureUrl: identity.pictureUrl
  });
}

function buildUserIdentityCreatePayload(identity: IExternalUserIdentity): {
  provider: string;
  providerSubject: string;
  emailAtLogin?: string;
} {
  return removeUndefinedValues({
    ...buildIdentityLookup(identity),
    emailAtLogin: identity.email
  });
}

function buildIdentityLookup(identity: IExternalUserIdentity): {
  provider: string;
  providerSubject: string;
} {
  return {
    provider: identity.provider ?? COGNITO_NATIVE_PROVIDER,
    providerSubject: identity.cognitoSub
  };
}

function buildUserReadArgs(options?: { selectIdOnly?: boolean }):
  | { select: { id: true } }
  | { include: { identities: true } } {
  if (options?.selectIdOnly) {
    return { select: { id: true } };
  }

  return {
    include: {
      identities: true
    }
  };
}

function providersFromUser(user: IUserPersistenceRecord): string[] {
  return Array.from(
    new Set(user.identities?.map((identity) => identity.provider) ?? [])
  );
}

function isPrismaUniqueConstraintError(error: unknown): error is { code: 'P2002' } {
  return (
    typeof error === 'object' &&
    error !== null &&
    'code' in error &&
    error.code === 'P2002'
  );
}

function removeUndefinedValues<T extends Record<string, unknown>>(value: T): T {
  return Object.fromEntries(
    Object.entries(value).filter(([, entryValue]) => entryValue !== undefined)
  ) as T;
}

export async function grantDemoOwnerRoleIfEligible(
  client: IUserRoleUpsertClient,
  userId: string,
  identity: IExternalUserIdentity
): Promise<boolean> {
  if (!shouldProvisionDemoOwnerRole(identity)) {
    return false;
  }

  await upsertOwnerRole(client, userId);

  return true;
}

export async function upsertOwnerRole(
  client: IUserRoleUpsertClient,
  userId: string
): Promise<void> {
  await client.userRole.upsert({
    where: {
      userId_role: {
        userId,
        role: 'OWNER'
      }
    },
    create: {
      userId,
      role: 'OWNER'
    },
    update: {}
  });
}

export async function hasPersistedOwnerRole(
  client: IUserRoleProvisioningClient,
  userId: string
): Promise<boolean> {
  const ownerRole = await client.userRole.findUnique({
    where: {
      userId_role: {
        userId,
        role: 'OWNER'
      }
    },
    select: { id: true }
  });

  return ownerRole !== null;
}
