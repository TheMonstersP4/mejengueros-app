import type { INotificationOutput } from '../dto/notification.output';

/**
 * Realtime delivery boundary for notifications that already exist in storage.
 */
export interface INotificationRealtimePublisher {
  publish(userId: string, notification: INotificationOutput): Promise<void>;
}

/**
 * Dependency injection token for notification realtime delivery.
 */
export const NOTIFICATION_REALTIME_PUBLISHER = Symbol(
  'NOTIFICATION_REALTIME_PUBLISHER'
);
