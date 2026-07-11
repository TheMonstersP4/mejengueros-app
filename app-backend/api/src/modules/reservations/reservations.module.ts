import { Module } from '@nestjs/common';
import { FilesModule } from '../files/files.module';
import { CLOCK } from '@/shared/application/clock/clock.port';
import { SystemClock } from '@/shared/infrastructure/clock/system-clock.service';
import { AuthModule } from '../auth/auth.module';
import { UsersModule } from '../users/users.module';
import { CreateReservationUseCase } from './application/use-cases/create-reservation.use-case';
import { CompleteExpiredReservationsUseCase } from './application/use-cases/complete-expired-reservations.use-case';
import { GetReservableDaysUseCase } from './application/use-cases/get-reservable-days.use-case';
import { GetReservableSlotsUseCase } from './application/use-cases/get-reservable-slots.use-case';
import { ListMyReservationsUseCase } from './application/use-cases/list-my-reservations.use-case';
import { ListOwnerReservationsUseCase } from './application/use-cases/list-owner-reservations.use-case';
import { RESERVATION_REPOSITORY } from './domain/repositories/reservation.repository';
import { PrismaReservationRepository } from './infrastructure/persistence/prisma-reservation.repository';
import { OwnerReservationsController } from './interfaces/http/controllers/owner-reservations.controller';
import { ReservableDaysController } from './interfaces/http/controllers/reservable-days.controller';
import { ReservationsController } from './interfaces/http/controllers/reservations.controller';
import { ReservableSlotsController } from './interfaces/http/controllers/reservable-slots.controller';

@Module({
  imports: [AuthModule, UsersModule, FilesModule],
  controllers: [
    ReservationsController,
    OwnerReservationsController,
    ReservableSlotsController,
    ReservableDaysController
  ],
  providers: [
    CreateReservationUseCase,
    CompleteExpiredReservationsUseCase,
    GetReservableDaysUseCase,
    GetReservableSlotsUseCase,
    ListMyReservationsUseCase,
    ListOwnerReservationsUseCase,
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
