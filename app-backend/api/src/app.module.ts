import 'dotenv/config';
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { APP_FILTER, APP_INTERCEPTOR } from '@nestjs/core';
import { LoggerModule } from 'nestjs-pino';
import { configuration } from './config/configuration';
import { validateEnv } from './config/env.validation';
import { loggerConfig } from './config/logger.config';
import { AuthModule } from './modules/auth/auth.module';
import { ComplexesModule } from './modules/complexes/complexes.module';
import { FilesModule } from './modules/files/files.module';
import { HealthModule } from './modules/health/health.module';
import { LocationsModule } from './modules/locations/locations.module';
import { ServiceCatalogModule } from './modules/service-catalog/service-catalog.module';
import { UsersModule } from './modules/users/users.module';
import { PrismaModule } from './shared/infrastructure/database/prisma.module';
import { ApiExceptionFilter } from './shared/interfaces/http/filters/api-exception.filter';
import { ApiResponseInterceptor } from './shared/interfaces/http/interceptors/api-response.interceptor';

const databaseBackedModules = process.env.DATABASE_URL
  ? [
      PrismaModule,
      UsersModule,
      ComplexesModule,
      LocationsModule,
      ServiceCatalogModule
    ]
  : [];

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      load: [configuration],
      validate: validateEnv
    }),
    LoggerModule.forRoot(loggerConfig()),
    HealthModule,
    AuthModule,
    FilesModule,
    ...databaseBackedModules
  ],
  providers: [
    {
      provide: APP_FILTER,
      useClass: ApiExceptionFilter
    },
    {
      provide: APP_INTERCEPTOR,
      useClass: ApiResponseInterceptor
    }
  ]
})
export class AppModule {}
