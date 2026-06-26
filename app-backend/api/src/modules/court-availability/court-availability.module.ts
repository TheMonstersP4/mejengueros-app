import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { GetCourtAvailabilityUseCase } from './application/use-cases/get-court-availability.use-case';
import { SaveCourtAvailabilityUseCase } from './application/use-cases/save-court-availability.use-case';
import { COURT_AVAILABILITY_REPOSITORY } from './domain/repositories/court-availability.repository';
import { PrismaCourtAvailabilityRepository } from './infrastructure/persistence/prisma-court-availability.repository';
import { CourtAvailabilityController } from './interfaces/http/controllers/court-availability.controller';

@Module({
  imports: [AuthModule],
  controllers: [CourtAvailabilityController],
  providers: [
    GetCourtAvailabilityUseCase,
    SaveCourtAvailabilityUseCase,
    {
      provide: COURT_AVAILABILITY_REPOSITORY,
      useClass: PrismaCourtAvailabilityRepository
    }
  ]
})
export class CourtAvailabilityModule {}
