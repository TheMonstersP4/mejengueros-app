import { NestFactory } from '@nestjs/core';
import type {
  NestFastifyApplication
} from '@nestjs/platform-fastify';
import {
  FastifyAdapter
} from '@nestjs/platform-fastify';
import { ConfigService } from '@nestjs/config';
import { Logger } from 'nestjs-pino';
import { loadDatabaseUrlFromSecret } from './database-secret';

/**
 * Fastify bootstrap for the HTTP API.
 *
 * @remarks
 * HTTP startup stays outside feature modules so DDD layers remain focused
 * on application behavior.
 */
export async function createFastifyApp(): Promise<NestFastifyApplication> {
  await loadDatabaseUrlFromSecret();
  const { AppModule } = await import('../app.module');

  const app = await NestFactory.create<NestFastifyApplication>(
    AppModule,
    new FastifyAdapter({ logger: false }),
    { bufferLogs: true }
  );

  app.useLogger(app.get(Logger));
  app.setGlobalPrefix('v1');
  const corsAllowedOrigins = app
    .get(ConfigService)
    .get<string[]>('app.corsAllowedOrigins', []);

  if (corsAllowedOrigins.length > 0) {
    app.enableCors({
      origin: corsAllowedOrigins,
      methods: ['GET', 'POST', 'OPTIONS'],
      allowedHeaders: ['Authorization', 'Content-Type'],
      maxAge: 300
    });
  }

  return app;
}
