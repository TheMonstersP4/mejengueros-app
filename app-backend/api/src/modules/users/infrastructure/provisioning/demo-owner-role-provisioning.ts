import type { IExternalUserIdentity } from '../../domain/repositories/user.repository';

interface IUserIdentityPersistenceClient<TResult extends { id: string }> {
  user: {
    upsert(args: {
      where: { cognitoSub: string };
      create: {
        cognitoSub: string;
        email: string;
        name?: string;
        pictureUrl?: string;
        provider?: string;
      };
      update: {
        email?: string;
        name?: string;
        pictureUrl?: string;
        provider?: string;
      };
      select?: { id: true };
    }): Promise<TResult>;
  };
}

interface IUserRoleProvisioningClient {
  userRole: {
    findUnique(args: {
      where: {
        userId_role: {
          userId: string;
          role: 'OWNER';
        };
      };
      select: { id: true };
    }): Promise<{ id: string } | null>;
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
  options?: { selectIdOnly?: boolean }
): Promise<TResult> {
  return client.user.upsert({
    where: { cognitoSub: identity.cognitoSub },
    create: {
      cognitoSub: identity.cognitoSub,
      email: identity.email ?? `${identity.cognitoSub}@unknown.local`,
      name: identity.name,
      pictureUrl: identity.pictureUrl,
      provider: identity.provider
    },
    update: {
      email: identity.email,
      name: identity.name,
      pictureUrl: identity.pictureUrl,
      provider: identity.provider
    },
    ...(options?.selectIdOnly ? { select: { id: true } } : {})
  });
}

export async function grantDemoOwnerRoleIfEligible(
  client: IUserRoleProvisioningClient,
  userId: string,
  identity: IExternalUserIdentity
): Promise<boolean> {
  if (!shouldProvisionDemoOwnerRole(identity)) {
    return false;
  }

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

  return true;
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
