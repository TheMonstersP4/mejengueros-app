import type { INestApplication } from '@nestjs/common';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';
import type { OpenAPIObject } from '@nestjs/swagger';

/**
 * Builds the OpenAPI document configuration.
 *
 * @returns Swagger document configuration.
 */
export function createSwaggerConfig(): Omit<OpenAPIObject, 'paths'> {
  return new DocumentBuilder()
    .setTitle('Mejengueros API')
    .setDescription('HTTP API for authentication, users, and image uploads.')
    .setVersion('0.1.0')
    .addBearerAuth({
      bearerFormat: 'JWT',
      description: 'Cognito ID token sent as: Bearer <token>',
      scheme: 'bearer',
      type: 'http'
    })
    .build();
}

/**
 * Registers OpenAPI documentation routes.
 *
 * @param app - NestJS application instance.
 */
export function configureSwagger(app: INestApplication): void {
  const config = createSwaggerConfig();
  const document = SwaggerModule.createDocument(app, config);

  SwaggerModule.setup('docs', app, document, {
    jsonDocumentUrl: 'openapi.json'
  });
}
