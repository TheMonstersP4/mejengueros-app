import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { FilesModule } from '../files/files.module';
import { CreateCourtForOwnedComplexUseCase } from './application/use-cases/create-court-for-owned-complex.use-case';
import { CreateComplexWithFirstCourtUseCase } from './application/use-cases/create-complex-with-first-court.use-case';
import { GetMyComplexHubUseCase } from './application/use-cases/get-my-complex-hub.use-case';
import { UpdateOwnedCourtImageUseCase } from './application/use-cases/update-owned-court-image.use-case';
import { COMPLEX_REPOSITORY } from './domain/repositories/complex.repository';
import { PrismaComplexRepository } from './infrastructure/persistence/prisma-complex.repository';
import { ComplexesController } from './interfaces/http/controllers/complexes.controller';

/**
 * Feature module for sports complex management.
 */
@Module({
  imports: [AuthModule, FilesModule],
  controllers: [ComplexesController],
  providers: [
    CreateComplexWithFirstCourtUseCase,
    CreateCourtForOwnedComplexUseCase,
    UpdateOwnedCourtImageUseCase,
    GetMyComplexHubUseCase,
    {
      provide: COMPLEX_REPOSITORY,
      useClass: PrismaComplexRepository
    }
  ]
})
export class ComplexesModule {}
