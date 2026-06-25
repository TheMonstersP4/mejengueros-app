import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { ListActiveServicesUseCase } from './application/use-cases/list-active-services.use-case';
import { SERVICE_CATALOG_REPOSITORY } from './domain/repositories/service-catalog.repository';
import { PrismaServiceCatalogRepository } from './infrastructure/persistence/prisma-service-catalog.repository';
import { ServiceCatalogController } from './interfaces/http/controllers/service-catalog.controller';

/**
 * Feature module for active service catalogs used by the complex wizard.
 */
@Module({
  imports: [AuthModule],
  controllers: [ServiceCatalogController],
  providers: [
    ListActiveServicesUseCase,
    {
      provide: SERVICE_CATALOG_REPOSITORY,
      useClass: PrismaServiceCatalogRepository
    }
  ]
})
export class ServiceCatalogModule {}
