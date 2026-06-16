import {
  GetSecretValueCommand,
  SecretsManagerClient
} from '@aws-sdk/client-secrets-manager';

const DATABASE_URL_SECRET_KEYS = ['DATABASE_URL', 'databaseUrl', 'url'];

/**
 * Loads `DATABASE_URL` from AWS Secrets Manager when the value is not already
 * present in the process environment.
 */
export async function loadDatabaseUrlFromSecret(): Promise<void> {
  if (process.env.DATABASE_URL || !process.env.DATABASE_SECRET_ARN) {
    return;
  }

  const client = new SecretsManagerClient({
    region: process.env.AWS_REGION
  });

  const response = await client.send(
    new GetSecretValueCommand({
      SecretId: process.env.DATABASE_SECRET_ARN
    })
  );

  const secretValue = readSecretValue(response.SecretString, response.SecretBinary);
  process.env.DATABASE_URL = parseDatabaseUrlSecret(secretValue);
}

/**
 * Reads a Secrets Manager payload as text.
 *
 * @param secretString - Plain string secret value.
 * @param secretBinary - Binary secret value.
 * @returns Secret payload as text.
 */
function readSecretValue(
  secretString: string | undefined,
  secretBinary: Uint8Array | undefined
): string {
  if (secretString) {
    return secretString;
  }

  if (secretBinary) {
    return Buffer.from(secretBinary).toString('utf8');
  }

  throw new Error('Database secret does not contain a value.');
}

/**
 * Extracts the database URL from either a raw URL secret or a small JSON object.
 *
 * @param secretValue - Secret payload from Secrets Manager.
 * @returns PostgreSQL connection URL.
 */
function parseDatabaseUrlSecret(secretValue: string): string {
  const trimmedSecret = secretValue.trim();

  if (trimmedSecret.startsWith('{')) {
    const parsedSecret = JSON.parse(trimmedSecret) as Record<string, unknown>;

    for (const key of DATABASE_URL_SECRET_KEYS) {
      const value = parsedSecret[key];

      if (typeof value === 'string' && value.trim() !== '') {
        return value;
      }
    }

    throw new Error('Database secret JSON does not contain a database URL.');
  }

  if (trimmedSecret === '') {
    throw new Error('Database secret is empty.');
  }

  return trimmedSecret;
}
