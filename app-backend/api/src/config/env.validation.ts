import { z } from 'zod';
import {
  PROFILE_IMAGE_DEFAULT_MAX_BYTES,
  PROFILE_IMAGE_HARD_MAX_BYTES
} from '../modules/files/domain/constants/image-upload.constants';
import {
  AppEnvironment,
  AppLogLevel,
  DEFAULT_APP_ENVIRONMENT,
  DEFAULT_HTTP_PORT,
  DEFAULT_LOG_LEVEL
} from './runtime.constants';

const envSchema = z.object({
  NODE_ENV: z.nativeEnum(AppEnvironment).default(DEFAULT_APP_ENVIRONMENT),
  PORT: z.coerce.number().int().positive().default(DEFAULT_HTTP_PORT),
  LOG_LEVEL: z.nativeEnum(AppLogLevel).default(DEFAULT_LOG_LEVEL),
  ERROR_DOCUMENTATION_BASE_URL: z.preprocess(
    (value) => (value === '' ? undefined : value),
    z.string().url().optional()
  ),
  AWS_REGION: z.string().min(1),
  APP_CORS_ALLOWED_ORIGINS: z.string().default(''),
  DATABASE_URL: z.preprocess(
    (value) => (value === '' ? undefined : value),
    z.string().min(1).optional()
  ),
  DATABASE_SECRET_ARN: z.preprocess(
    (value) => (value === '' ? undefined : value),
    z.string().min(1).optional()
  ),
  COGNITO_USER_POOL_ID: z.string().min(1),
  COGNITO_CLIENT_ID: z.string().min(1),
  COGNITO_TOKEN_USE: z.enum(['access', 'id']).default('id'),
  DEMO_OWNER_EMAILS: z.string().default(''),
  DEMO_OWNER_SUBS: z.string().default(''),
  APP_S3_BUCKET_NAME: z.string().min(1),
  APP_S3_REGION: z.string().min(1).optional(),
  APP_S3_KEY_PREFIX: z.string().min(1).default('uploads'),
  APP_S3_UPLOAD_URL_TTL_SECONDS: z.coerce
    .number()
    .int()
    .positive()
    .max(900)
    .default(300),
  APP_S3_PROFILE_IMAGE_MAX_BYTES: z.coerce
    .number()
    .int()
    .positive()
    .max(PROFILE_IMAGE_HARD_MAX_BYTES)
    .default(PROFILE_IMAGE_DEFAULT_MAX_BYTES),
  APP_S3_ALLOWED_IMAGE_MIME_TYPES: z
    .string()
    .min(1)
    .default('image/jpeg,image/png,image/webp'),
  WEBSOCKET_CONNECTIONS_TABLE_NAME: z.string().min(1),
  WEBSOCKET_CONNECTION_TTL_SECONDS: z.coerce.number().int().positive().default(86400)
});

/**
 * Validates environment variables during application startup.
 *
 * @param config - Raw environment values received by `@nestjs/config`.
 * @returns Parsed environment values.
 * @throws ZodError when required variables are missing or invalid.
 */
export function validateEnv(config: Record<string, unknown>): Record<string, unknown> {
  return envSchema.parse(config);
}
