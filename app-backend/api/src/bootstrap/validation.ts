import type { INestApplication } from '@nestjs/common';
import { ValidationPipe } from '@nestjs/common';

/**
 * Configures global request DTO validation.
 *
 * @remarks
 * Unknown properties are rejected to avoid silently accepting unexpected input
 * at the HTTP boundary.
 *
 * @param app - NestJS application instance.
 */
export function configureValidation(app: INestApplication): void {
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true
    })
  );
}
