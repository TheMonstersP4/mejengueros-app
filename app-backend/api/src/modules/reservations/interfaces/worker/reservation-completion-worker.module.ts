import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { configuration } from '@/config/configuration';
import { validateWorkerEnv } from '@/config/worker-env.validation';
import { CLOCK } from '@/shared/application/clock/clock.port';
import { SystemClock } from '@/shared/infrastructure/clock/system-clock.service';
import { PrismaModule } from '@/shared/infrastructure/database/prisma.module';
import { NotificationDeliveryModule } from '@/modules/notifications/notification-delivery.module';
import { CompleteExpiredReservationsUseCase } from '../../application/use-cases/complete-expired-reservations.use-case';
import { RESERVATION_REPOSITORY } from '../../domain/repositories/reservation.repository';
import { PrismaReservationRepository } from '../../infrastructure/persistence/prisma-reservation.repository';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      load: [configuration],
      validate: validateWorkerEnv
    }),
    PrismaModule,
    NotificationDeliveryModule
  ],
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
