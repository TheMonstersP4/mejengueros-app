import { Inject, Injectable } from '@nestjs/common';
import type { ICompletedReservationSnapshot } from '@/modules/reservations/domain/repositories/reservation.repository';
import { presentNotification } from '../services/notification-presenter';
import {
  NOTIFICATION_REALTIME_PUBLISHER,
  type INotificationRealtimePublisher
} from '../ports/notification-realtime-publisher.port';
import {
  NOTIFICATION_REPOSITORY,
  type INotificationRepository
} from '../../domain/repositories/notification.repository';

/**
 * Creates review prompt notifications for newly completed reservations.
 */
@Injectable()
export class CreateReviewPromptNotificationsUseCase {
  constructor(
    @Inject(NOTIFICATION_REPOSITORY)
    private readonly notificationRepository: INotificationRepository,
    @Inject(NOTIFICATION_REALTIME_PUBLISHER)
    private readonly realtimePublisher: INotificationRealtimePublisher
  ) {}

  /**
   * Persists review prompts and attempts realtime delivery for connected users.
   *
   * @param reservations - Reservations completed by the current worker run.
   * @returns Number of notifications created in storage.
   */
  async execute(reservations: ICompletedReservationSnapshot[]): Promise<number> {
    const notifications =
      await this.notificationRepository.createReviewPromptNotifications(reservations);

    await Promise.all(
      notifications.map(async (notification) => {
        try {
          await this.realtimePublisher.publish(
            notification.userId,
            presentNotification(notification)
          );
        } catch {
          // Persistence is the delivery guarantee; realtime is best-effort.
        }
      })
    );

    return notifications.length;
  }
}
