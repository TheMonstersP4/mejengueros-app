import type { INotificationOutput } from '../dto/notification.output';
import type { INotificationSnapshot } from '../../domain/repositories/notification.repository';

/**
 * Maps persisted notification records into the public API contract.
 */
export function presentNotification(
  notification: INotificationSnapshot
): INotificationOutput {
  return {
    id: notification.id,
    type: notification.type,
    status: notification.status,
    reservationId: notification.reservationId,
    title: 'Contanos como estuvo la mejenga',
    message: `Tu reserva en ${notification.complexName} - ${notification.courtName} ya termino. Dejanos tu resena.`,
    reservation: {
      id: notification.reservationId,
      courtId: notification.courtId,
      complexId: notification.complexId,
      complexName: notification.complexName,
      courtName: notification.courtName,
      startsAt: notification.startsAt,
      endsAt: notification.endsAt
    },
    reviewId: notification.reviewId,
    action: {
      type: 'OPEN_REVIEW',
      reservationId: notification.reservationId
    },
    createdAt: notification.createdAt,
    readAt: notification.readAt
  };
}
