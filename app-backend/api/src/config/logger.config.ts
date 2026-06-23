import type { Params } from 'nestjs-pino';
import {
  AppEnvironment,
  type AppLogLevel,
  readAppEnvironment,
  readAppLogLevel
} from './runtime.constants';

/**
 * Logger settings loaded from environment variables.
 */
export interface ILoggerAppConfig {
  /**
   * Minimum log level emitted by Pino.
   */
  level: AppLogLevel;
}

/**
 * Loads logger application config.
 *
 * @returns Logger config section used by `configuration`.
 */
export function loggerAppConfig(): ILoggerAppConfig {
  return {
    level: readAppLogLevel(process.env.LOG_LEVEL)
  };
}

/**
 * Builds the `nestjs-pino` logger configuration.
 *
 * @remarks
 * Sensitive request headers are redacted and `pino-pretty` is enabled only for
 * local development.
 *
 * @returns Pino module parameters.
 */
export function loggerConfig(): Params {
  const level = readAppLogLevel(process.env.LOG_LEVEL);
  const environment = readAppEnvironment(process.env.NODE_ENV);
  const usePrettyLogs = environment === AppEnvironment.Development;

  return {
    pinoHttp: {
      level,
      redact: ['req.headers.authorization', 'req.headers.cookie'],
      transport: usePrettyLogs
        ? {
            target: 'pino-pretty',
            options: {
              singleLine: true,
              colorize: true
            }
          }
        : undefined
    }
  };
}
