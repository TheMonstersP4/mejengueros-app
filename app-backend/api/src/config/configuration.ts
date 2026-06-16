import { appConfig } from './app.config';
import { authConfig } from './auth.config';
import { databaseConfig } from './database.config';
import { loggerAppConfig } from './logger.config';
import { storageConfig } from './storage.config';
import { websocketConfig } from './websocket.config';

/**
 * Root configuration object registered in NestJS.
 */
export interface IApplicationConfiguration {
  app: ReturnType<typeof appConfig>;
  auth: ReturnType<typeof authConfig>;
  database: ReturnType<typeof databaseConfig>;
  logger: ReturnType<typeof loggerAppConfig>;
  storage: ReturnType<typeof storageConfig>;
  websocket: ReturnType<typeof websocketConfig>;
}

/**
 * Builds the full application configuration object.
 *
 * @returns Config sections loaded by `@nestjs/config`.
 */
export function configuration(): IApplicationConfiguration {
  return {
    app: appConfig(),
    auth: authConfig(),
    database: databaseConfig(),
    logger: loggerAppConfig(),
    storage: storageConfig(),
    websocket: websocketConfig()
  };
}
