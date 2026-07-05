import { Module } from '@nestjs/common';
import { CLOCK } from '@/shared/application/clock/clock.port';
import { SystemClock } from '@/shared/infrastructure/clock/system-clock.service';
import { AuthModule } from '../auth/auth.module';
import { UsersModule } from '../users/users.module';
import { CreateReservationUseCase } from './application/use-cases/create-reservation.use-case';
import { GetReservableDaysUseCase } from './application/use-cases/get-reservable-days.use-case';
import { GetReservableSlotsUseCase } from './application/use-cases/get-reservable-slots.use-case';
import { RESERVATION_REPOSITORY } from './domain/repositories/reservation.repository';
import { PrismaReservationRepository } from './infrastructure/persistence/prisma-reservation.repository';
import { ReservableDaysController } from './interfaces/http/controllers/reservable-days.controller';
import { ReservationsController } from './interfaces/http/controllers/reservations.controller';
import { ReservableSlotsController } from './interfaces/http/controllers/reservable-slots.controller';

@Module({
  imports: [AuthModule, UsersModule],
  controllers: [
    ReservationsController,
    ReservableSlotsController,
    ReservableDaysController
  ],
  providers: [
    CreateReservationUseCase,
    GetReservableDaysUseCase,
    GetReservableSlotsUseCase,
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
export class ReservationsModule {}
