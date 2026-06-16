import 'reflect-metadata';
import { createFastifyApp } from './bootstrap/fastify';
import { configureShutdown } from './bootstrap/shutdown';
import { configureSwagger } from './bootstrap/swagger';
import { configureValidation } from './bootstrap/validation';
import { configuration } from './config/configuration';

async function bootstrap(): Promise<void> {
  const app = await createFastifyApp();
  const config = configuration();

  configureValidation(app);
  configureSwagger(app);
  configureShutdown(app);

  await app.listen({ host: '0.0.0.0', port: config.app.port });
}

void bootstrap();
