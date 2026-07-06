import { Module } from '@nestjs/common';
import { CLOCK } from '@/shared/application/clock/clock.port';
import { SystemClock } from '@/shared/infrastructure/clock/system-clock.service';
import { PrismaModule } from '@/shared/infrastructure/database/prisma.module';
import { CompleteExpiredReservationsUseCase } from '../../application/use-cases/complete-expired-reservations.use-case';
import { RESERVATION_REPOSITORY } from '../../domain/repositories/reservation.repository';
import { PrismaReservationRepository } from '../../infrastructure/persistence/prisma-reservation.repository';

@Module({
  imports: [PrismaModule],
  providers: [
    CompleteExpiredReservationsUseCase,
    SystemClock,
    {
      provide: CLOCK,
      useExisting: SystemClock
    },
    {
      provide: RESERVATION_REPOSITORY,
      useClass: PrismaReservationRepository
    }
  ]
})
export class ReservationCompletionWorkerModule {}
