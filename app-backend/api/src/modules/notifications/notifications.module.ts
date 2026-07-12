import { Module } from '@nestjs/common';
import { PrismaModule } from '@/shared/infrastructure/database/prisma.module';
import { AuthModule } from '../auth/auth.module';
import { UsersModule } from '../users/users.module';
import { CreateReviewPromptNotificationsUseCase } from './application/use-cases/create-review-prompt-notifications.use-case';
import { ListMyNotificationsUseCase } from './application/use-cases/list-my-notifications.use-case';
import { MarkNotificationReadUseCase } from './application/use-cases/mark-notification-read.use-case';
import { NOTIFICATION_REALTIME_PUBLISHER } from './application/ports/notification-realtime-publisher.port';
import { NOTIFICATION_REPOSITORY } from './domain/repositories/notification.repository';
import { PrismaNotificationRepository } from './infrastructure/persistence/prisma-notification.repository';
import { ApiGatewayNotificationRealtimePublisher } from './infrastructure/realtime/api-gateway-notification-realtime.publisher';
import { NotificationsController } from './interfaces/http/controllers/notifications.controller';

/**
 * Feature module for persisted and realtime user notifications.
 */
@Module({
  imports: [AuthModule, UsersModule, PrismaModule],
  controllers: [NotificationsController],
  providers: [
    CreateReviewPromptNotificationsUseCase,
    ListMyNotificationsUseCase,
    MarkNotificationReadUseCase,
    {
      provide: NOTIFICATION_REPOSITORY,
      useClass: PrismaNotificationRepository
    },
    {
      provide: NOTIFICATION_REALTIME_PUBLISHER,
      useClass: ApiGatewayNotificationRealtimePublisher
    }
  ],
  exports: [CreateReviewPromptNotificationsUseCase]
})
export class NotificationsModule {}
