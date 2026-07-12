import type {
  NotificationStatus,
  NotificationType
} from '@/generated/prisma/enums';

/**
 * Client action attached to an application notification.
 */
export interface INotificationActionOutput {
  type: 'OPEN_REVIEW';
  reservationId: string;
}

/**
 * Reservation context needed by clients to open the review flow.
 */
export interface INotificationReservationOutput {
  id: string;
  complexName: string;
  courtName: string;
  startsAt: string;
  endsAt: string;
}

/**
 * Notification payload returned by the API and emitted through realtime channels.
 */
export interface INotificationOutput {
  id: string;
  type: NotificationType;
  status: NotificationStatus;
  reservationId: string;
  title: string;
  message: string;
  reservation: INotificationReservationOutput;
  action: INotificationActionOutput;
  createdAt: string;
  readAt: string | null;
}
