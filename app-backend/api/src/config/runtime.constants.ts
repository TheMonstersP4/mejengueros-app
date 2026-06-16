/**
 * Runtime environments accepted by the application.
 */
export enum AppEnvironment {
  Development = 'development',
  Test = 'test',
  Production = 'production'
}

/**
 * Log levels supported by Pino.
 */
export enum AppLogLevel {
  Trace = 'trace',
  Debug = 'debug',
  Info = 'info',
  Warn = 'warn',
  Error = 'error',
  Fatal = 'fatal',
  Silent = 'silent'
}

/**
 * Default runtime environment used when `NODE_ENV` is omitted.
 */
export const DEFAULT_APP_ENVIRONMENT = AppEnvironment.Development;

/**
 * Default Pino log level used when `LOG_LEVEL` is omitted.
 */
export const DEFAULT_LOG_LEVEL = AppLogLevel.Info;

/**
 * Default HTTP port used by the NestJS app.
 */
export const DEFAULT_HTTP_PORT = 3000;

/**
 * Returns whether the app is running in production mode.
 *
 * @param environment - Runtime environment value.
 * @returns True when the environment is production.
 */
export function isProductionEnvironment(environment: AppEnvironment): boolean {
  return environment === AppEnvironment.Production;
}

/**
 * Parses a raw runtime environment value.
 *
 * @param value - Raw `NODE_ENV` value.
 * @returns Supported runtime environment or the default.
 */
export function readAppEnvironment(value?: string): AppEnvironment {
  return Object.values(AppEnvironment).includes(value as AppEnvironment)
    ? (value as AppEnvironment)
    : DEFAULT_APP_ENVIRONMENT;
}

/**
 * Parses a raw Pino log level value.
 *
 * @param value - Raw `LOG_LEVEL` value.
 * @returns Supported Pino log level or the default.
 */
export function readAppLogLevel(value?: string): AppLogLevel {
  return Object.values(AppLogLevel).includes(value as AppLogLevel)
    ? (value as AppLogLevel)
    : DEFAULT_LOG_LEVEL;
}
