import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { ListCantonsByProvinceUseCase } from './application/use-cases/list-cantons-by-province.use-case';
import { ListProvincesUseCase } from './application/use-cases/list-provinces.use-case';
import { LOCATION_CATALOG_REPOSITORY } from './domain/repositories/location-catalog.repository';
import { PrismaLocationCatalogRepository } from './infrastructure/persistence/prisma-location-catalog.repository';
import { LocationsController } from './interfaces/http/controllers/locations.controller';

/**
 * Feature module for controlled wizard location catalogs.
 */
@Module({
  imports: [AuthModule],
  controllers: [LocationsController],
  providers: [
    ListProvincesUseCase,
    ListCantonsByProvinceUseCase,
    {
      provide: LOCATION_CATALOG_REPOSITORY,
      useClass: PrismaLocationCatalogRepository
    }
  ]
})
export class LocationsModule {}
