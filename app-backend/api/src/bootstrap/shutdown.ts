import type { INestApplication } from '@nestjs/common';

/**
 * Enables graceful shutdown hooks for NestJS providers.
 *
 * @param app - NestJS application instance.
 */
export function configureShutdown(app: INestApplication): void {
  app.enableShutdownHooks();
}
