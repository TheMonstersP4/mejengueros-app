import { Module } from '@nestjs/common';
import { ListPublicCourtCatalogUseCase } from './application/use-cases/list-public-court-catalog.use-case';
import { COURT_CATALOG_REPOSITORY } from './domain/repositories/court-catalog.repository';
import { PrismaCourtCatalogRepository } from './infrastructure/persistence/prisma-court-catalog.repository';
import { CourtsController } from './interfaces/http/controllers/courts.controller';

@Module({
  controllers: [CourtsController],
  providers: [
    ListPublicCourtCatalogUseCase,
    {
      provide: COURT_CATALOG_REPOSITORY,
      useClass: PrismaCourtCatalogRepository
    }
  ]
})
export class CourtsModule {}
