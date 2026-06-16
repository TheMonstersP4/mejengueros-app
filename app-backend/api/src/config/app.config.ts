import {
  DEFAULT_HTTP_PORT,
  type AppEnvironment,
  readAppEnvironment
} from './runtime.constants';

/**
 * Application runtime settings.
 */
export interface IAppConfig {
  /**
   * Current runtime environment.
   */
  environment: AppEnvironment;

  /**
   * HTTP port used by the NestJS app.
   */
  port: number;

  /**
   * Browser origins allowed to call the HTTP API.
   */
  corsAllowedOrigins: string[];
}

/**
 * Loads application runtime settings.
 *
 * @returns Application config section.
 */
export function appConfig(): IAppConfig {
  return {
    environment: readAppEnvironment(process.env.NODE_ENV),
    port: Number(process.env.PORT ?? DEFAULT_HTTP_PORT),
    corsAllowedOrigins: (process.env.APP_CORS_ALLOWED_ORIGINS ?? '')
      .split(',')
      .map((origin) => origin.trim())
      .filter(Boolean)
  };
}
