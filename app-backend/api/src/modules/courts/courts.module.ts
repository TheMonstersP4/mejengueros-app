import { Module } from '@nestjs/common';
import { FilesModule } from '../files/files.module';
import { ListPublicCourtCatalogUseCase } from './application/use-cases/list-public-court-catalog.use-case';
import { COURT_CATALOG_REPOSITORY } from './domain/repositories/court-catalog.repository';
import {
  COURT_CATALOG_TODAY_PROVIDER,
  PrismaCourtCatalogRepository
} from './infrastructure/persistence/prisma-court-catalog.repository';
import { CourtsController } from './interfaces/http/controllers/courts.controller';

@Module({
  imports: [FilesModule],
  controllers: [CourtsController],
  providers: [
    ListPublicCourtCatalogUseCase,
    {
      provide: COURT_CATALOG_TODAY_PROVIDER,
      useValue: () => new Date()
    },
    {
      provide: COURT_CATALOG_REPOSITORY,
      useClass: PrismaCourtCatalogRepository
    }
  ]
})
export class CourtsModule {}
