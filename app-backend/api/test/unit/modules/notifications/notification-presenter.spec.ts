import { presentNotification } from '@/modules/notifications/application/services/notification-presenter';
import type { INotificationSnapshot } from '@/modules/notifications/domain/repositories/notification.repository';

function buildSnapshot(
  overrides: Partial<INotificationSnapshot> = {}
): INotificationSnapshot {
  return {
    id: 'notification-1',
    userId: 'user-1',
    reservationId: 'reservation-1',
    type: 'REVIEW_PROMPT',
    status: 'PENDING',
    courtId: 'court-1',
    complexId: 'complex-1',
    complexName: 'Mejengas CR',
    courtName: 'Cancha 1',
    startsAt: '2026-07-11T18:00:00.000Z',
    endsAt: '2026-07-11T19:00:00.000Z',
    reviewId: null,
    createdAt: '2026-07-11T19:01:00.000Z',
    readAt: null,
    ...overrides
  };
}

describe('presentNotification', () => {
  it('exposes the court identity so clients can open the court detail', () => {
    const output = presentNotification(buildSnapshot());

    expect(output.reservation.courtId).toBe('court-1');
    expect(output.reservation.complexId).toBe('complex-1');
  });

  it('returns a null reviewId when the reservation has not been reviewed yet', () => {
    const output = presentNotification(buildSnapshot());

    expect(output.reviewId).toBeNull();
  });

  it('exposes the reviewId so clients open the court detail instead of the create form', () => {
    const output = presentNotification(buildSnapshot({ reviewId: 'review-1' }));

    expect(output.reviewId).toBe('review-1');
  });
});
