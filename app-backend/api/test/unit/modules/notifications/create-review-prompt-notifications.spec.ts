import { CreateReviewPromptNotificationsUseCase } from '@/modules/notifications/application/use-cases/create-review-prompt-notifications.use-case';
import type { INotificationRealtimePublisher } from '@/modules/notifications/application/ports/notification-realtime-publisher.port';
import type { INotificationRepository } from '@/modules/notifications/domain/repositories/notification.repository';

describe('CreateReviewPromptNotificationsUseCase', () => {
  it('persists review prompt notifications and publishes them to connected users', async () => {
    const repository: jest.Mocked<INotificationRepository> = {
      createReviewPromptNotifications: jest.fn().mockResolvedValue([
        {
          id: 'notification-1',
          userId: 'user-1',
          reservationId: 'reservation-1',
          type: 'REVIEW_PROMPT',
          status: 'PENDING',
          complexName: 'Mejengas CR',
          courtName: 'Cancha 1',
          startsAt: '2026-07-11T18:00:00.000Z',
          endsAt: '2026-07-11T19:00:00.000Z',
          createdAt: '2026-07-11T19:01:00.000Z',
          readAt: null
        }
      ]),
      listForUser: jest.fn(),
      markRead: jest.fn()
    };
    const publisher: jest.Mocked<INotificationRealtimePublisher> = {
      publish: jest.fn().mockResolvedValue(undefined)
    };
    const useCase = new CreateReviewPromptNotificationsUseCase(repository, publisher);
    const completedReservations = [{ id: 'reservation-1', userId: 'user-1' }];

    await expect(useCase.execute(completedReservations)).resolves.toBe(1);

    expect(repository.createReviewPromptNotifications).toHaveBeenCalledWith(
      completedReservations
    );
    expect(publisher.publish).toHaveBeenCalledWith(
      'user-1',
      expect.objectContaining({
        id: 'notification-1',
        title: 'Contanos como estuvo la mejenga',
        action: {
          type: 'OPEN_REVIEW',
          reservationId: 'reservation-1'
        }
      })
    );
  });

  it('keeps persisted notifications when realtime delivery fails', async () => {
    const repository: jest.Mocked<INotificationRepository> = {
      createReviewPromptNotifications: jest.fn().mockResolvedValue([
        {
          id: 'notification-1',
          userId: 'user-1',
          reservationId: 'reservation-1',
          type: 'REVIEW_PROMPT',
          status: 'PENDING',
          complexName: 'Mejengas CR',
          courtName: 'Cancha 1',
          startsAt: '2026-07-11T18:00:00.000Z',
          endsAt: '2026-07-11T19:00:00.000Z',
          createdAt: '2026-07-11T19:01:00.000Z',
          readAt: null
        }
      ]),
      listForUser: jest.fn(),
      markRead: jest.fn()
    };
    const publisher: jest.Mocked<INotificationRealtimePublisher> = {
      publish: jest.fn().mockRejectedValue(new Error('connection gone'))
    };
    const useCase = new CreateReviewPromptNotificationsUseCase(repository, publisher);

    await expect(
      useCase.execute([{ id: 'reservation-1', userId: 'user-1' }])
    ).resolves.toBe(1);
  });
});
