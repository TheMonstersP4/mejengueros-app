import type {
  NotificationStatus,
  NotificationType
} from '@/generated/prisma/enums';

/**
 * Completed reservation that may produce a post-reservation review prompt.
 */
export interface IReviewPromptReservationCandidate {
  id: string;
  userId: string;
}

/**
 * Notification row enriched with reservation context for API presentation.
 */
export interface INotificationSnapshot {
  id: string;
  userId: string;
  reservationId: string;
  type: NotificationType;
  status: NotificationStatus;
  complexName: string;
  courtName: string;
  startsAt: string;
  endsAt: string;
  createdAt: string;
  readAt: string | null;
}

/**
 * Persistence boundary for user notifications.
 */
export interface INotificationRepository {
  createReviewPromptNotifications(
    reservations: IReviewPromptReservationCandidate[]
  ): Promise<INotificationSnapshot[]>;

  listForUser(userId: string): Promise<INotificationSnapshot[]>;

  markRead(notificationId: string, userId: string): Promise<INotificationSnapshot | null>;
}

/**
 * Dependency injection token for the notification repository port.
 */
export const NOTIFICATION_REPOSITORY = Symbol('NOTIFICATION_REPOSITORY');
