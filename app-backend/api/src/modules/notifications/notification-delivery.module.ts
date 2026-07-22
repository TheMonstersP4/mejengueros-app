import { Module } from '@nestjs/common';
import { PrismaModule } from '@/shared/infrastructure/database/prisma.module';
import { CreateReviewPromptNotificationsUseCase } from './application/use-cases/create-review-prompt-notifications.use-case';
import { NOTIFICATION_REALTIME_PUBLISHER } from './application/ports/notification-realtime-publisher.port';
import { NOTIFICATION_REPOSITORY } from './domain/repositories/notification.repository';
import { PrismaNotificationRepository } from './infrastructure/persistence/prisma-notification.repository';
import { ApiGatewayNotificationRealtimePublisher } from './infrastructure/realtime/api-gateway-notification-realtime.publisher';

@Module({
  imports: [PrismaModule],
  providers: [
    CreateReviewPromptNotificationsUseCase,
    {
      provide: NOTIFICATION_REPOSITORY,
      useClass: PrismaNotificationRepository
    },
    {
      provide: NOTIFICATION_REALTIME_PUBLISHER,
      useClass: ApiGatewayNotificationRealtimePublisher
    }
  ],
  exports: [
    CreateReviewPromptNotificationsUseCase,
    NOTIFICATION_REPOSITORY,
    NOTIFICATION_REALTIME_PUBLISHER
  ]
})
export class NotificationDeliveryModule {}
