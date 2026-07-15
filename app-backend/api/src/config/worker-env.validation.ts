import { z } from 'zod';
import {
  AppEnvironment,
  AppLogLevel,
  DEFAULT_APP_ENVIRONMENT,
  DEFAULT_LOG_LEVEL
} from './runtime.constants';

const workerEnvSchema = z
  .object({
    NODE_ENV: z.nativeEnum(AppEnvironment).default(DEFAULT_APP_ENVIRONMENT),
    LOG_LEVEL: z.nativeEnum(AppLogLevel).default(DEFAULT_LOG_LEVEL),
    ERROR_DOCUMENTATION_BASE_URL: z.preprocess(
      (value) => (value === '' ? undefined : value),
      z.string().url().optional()
    ),
    AWS_REGION: z.string().min(1),
    DATABASE_URL: z.preprocess(
      (value) => (value === '' ? undefined : value),
      z.string().min(1).optional()
    ),
    DATABASE_SECRET_ARN: z.preprocess(
      (value) => (value === '' ? undefined : value),
      z.string().min(1).optional()
    ),
    WEBSOCKET_CONNECTIONS_TABLE_NAME: z.string().default(''),
    WEBSOCKET_CONNECTIONS_USER_ID_INDEX_NAME: z.string().default('byUserId'),
    WEBSOCKET_ENDPOINT: z.string().default('')
  })
  .refine((value) => value.DATABASE_URL || value.DATABASE_SECRET_ARN, {
    message: 'DATABASE_URL or DATABASE_SECRET_ARN is required.',
    path: ['DATABASE_URL']
  });

/**
 * Validates environment variables required by scheduled workers.
 *
 * @param config - Raw environment values received by `@nestjs/config`.
 * @returns Parsed worker environment values.
 * @throws ZodError when required worker variables are missing or invalid.
 */
export function validateWorkerEnv(
  config: Record<string, unknown>
): Record<string, unknown> {
  return workerEnvSchema.parse(config);
}
