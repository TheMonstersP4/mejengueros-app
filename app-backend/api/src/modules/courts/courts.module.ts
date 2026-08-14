import { Module } from '@nestjs/common';
import { FilesModule } from '../files/files.module';
import { AuthModule } from '../auth/auth.module';
import { UsersModule } from '../users/users.module';
import { DeactivateCourtUseCase } from './application/use-cases/deactivate-court.use-case';
import { ListPublicCourtCatalogUseCase } from './application/use-cases/list-public-court-catalog.use-case';
import { ReactivateCourtUseCase } from './application/use-cases/reactivate-court.use-case';
import { COURT_ADMIN_REPOSITORY } from './domain/repositories/court-admin.repository';
import { COURT_CATALOG_REPOSITORY } from './domain/repositories/court-catalog.repository';
import { PrismaCourtAdminRepository } from './infrastructure/persistence/prisma-court-admin.repository';
import {
  COURT_CATALOG_TODAY_PROVIDER,
  PrismaCourtCatalogRepository
} from './infrastructure/persistence/prisma-court-catalog.repository';
import { CourtsController } from './interfaces/http/controllers/courts.controller';

@Module({
  imports: [AuthModule, FilesModule, UsersModule],
  controllers: [CourtsController],
  providers: [
    DeactivateCourtUseCase,
    ListPublicCourtCatalogUseCase,
    ReactivateCourtUseCase,
    {
      provide: COURT_ADMIN_REPOSITORY,
      useClass: PrismaCourtAdminRepository
    },
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
