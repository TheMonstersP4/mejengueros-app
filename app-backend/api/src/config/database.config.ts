/**
 * Database connection settings.
 */
export interface IDatabaseConfig {
  /**
   * PostgreSQL connection URL.
   */
  url: string;
  /**
   * AWS Secrets Manager secret ARN used to load the PostgreSQL URL.
   */
  secretArn: string;
}

/**
 * Loads database connection settings.
 *
 * @returns Database config section.
 */
export function databaseConfig(): IDatabaseConfig {
  return {
    url: process.env.DATABASE_URL ?? '',
    secretArn: process.env.DATABASE_SECRET_ARN ?? ''
  };
}
