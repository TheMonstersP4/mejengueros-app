import type { INestApplication } from '@nestjs/common';
import { DocumentBuilder, SwaggerModule } from '@nestjs/swagger';

/**
 * Registers OpenAPI documentation routes.
 *
 * @param app - NestJS application instance.
 */
export function configureSwagger(app: INestApplication): void {
  const config = new DocumentBuilder()
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
  const document = SwaggerModule.createDocument(app, config);

  SwaggerModule.setup('docs', app, document, {
    jsonDocumentUrl: 'openapi.json'
  });
}
