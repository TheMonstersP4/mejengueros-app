import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { UsersModule } from '../users/users.module';
import { ListMyNotificationsUseCase } from './application/use-cases/list-my-notifications.use-case';
import { MarkNotificationReadUseCase } from './application/use-cases/mark-notification-read.use-case';
import { NotificationDeliveryModule } from './notification-delivery.module';
import { NotificationsController } from './interfaces/http/controllers/notifications.controller';

/**
 * Feature module for persisted and realtime user notifications.
 */
@Module({
  imports: [AuthModule, UsersModule, NotificationDeliveryModule],
  controllers: [NotificationsController],
  providers: [
    ListMyNotificationsUseCase,
    MarkNotificationReadUseCase
  ],
  exports: [NotificationDeliveryModule]
})
export class NotificationsModule {}
