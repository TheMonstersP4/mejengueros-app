import { CompleteExpiredReservationsUseCase } from '@/modules/reservations/application/use-cases/complete-expired-reservations.use-case';
import type { IClock } from '@/shared/application/clock/clock.port';
import type { IReservationRepository } from '@/modules/reservations/domain/repositories/reservation.repository';
import { PrismaReservationRepository } from '@/modules/reservations/infrastructure/persistence/prisma-reservation.repository';
import type { CreateReviewPromptNotificationsUseCase } from '@/modules/notifications/application/use-cases/create-review-prompt-notifications.use-case';

describe('reservation completion worker behavior', () => {
  function fixedClock(value: string): IClock {
    return {
      now: () => new Date(value)
    };
  }

  function createReservationRepository(
    overrides: Partial<jest.Mocked<IReservationRepository>> = {}
  ): jest.Mocked<IReservationRepository> {
    return {
      getReservationWindow: jest.fn(),
      createConfirmedReservation: jest.fn(),
      findMyReservationsByUserId: jest.fn(),
      listOwnerReservations: jest.fn(),
      completeExpiredReservations: jest.fn().mockResolvedValue([]),
      ...overrides
    } as unknown as jest.Mocked<IReservationRepository>;
  }

  function createNotificationsUseCase(
    execute = jest.fn().mockResolvedValue(0)
  ): jest.Mocked<CreateReviewPromptNotificationsUseCase> {
    return { execute } as unknown as jest.Mocked<CreateReviewPromptNotificationsUseCase>;
  }

  it('completes expired confirmed reservations using the current clock time', async () => {
    const completedReservations = [
      { id: 'reservation-1', userId: 'user-1' },
      { id: 'reservation-2', userId: 'user-2' }
    ];
    const repository = createReservationRepository({
      completeExpiredReservations: jest.fn().mockResolvedValue(completedReservations)
    });
    const notifications = createNotificationsUseCase(jest.fn().mockResolvedValue(2));
    const useCase = new CompleteExpiredReservationsUseCase(
      repository,
      fixedClock('2026-07-05T20:00:00.000Z'),
      notifications
    );

    await expect(useCase.execute()).resolves.toEqual({
      completedReservationsCount: 2,
      reviewPromptNotificationsCreatedCount: 2
    });
    expect(repository.completeExpiredReservations).toHaveBeenCalledWith({
      now: new Date('2026-07-05T20:00:00.000Z')
    });
    expect(notifications.execute).toHaveBeenCalledWith(completedReservations);
  });

  it('leaves future confirmed reservations untouched when no rows have expired yet', async () => {
    const repository = createReservationRepository({
      completeExpiredReservations: jest.fn().mockResolvedValue([])
    });
    const notifications = createNotificationsUseCase();
    const useCase = new CompleteExpiredReservationsUseCase(
      repository,
      fixedClock('2026-07-05T18:59:59.000Z'),
      notifications
    );

    await expect(useCase.execute()).resolves.toEqual({
      completedReservationsCount: 0,
      reviewPromptNotificationsCreatedCount: 0
    });
  });

  it('is safe under duplicate invocations because reruns can complete zero additional rows', async () => {
    const repository = createReservationRepository({
      completeExpiredReservations: jest
        .fn()
        .mockResolvedValueOnce([{ id: 'reservation-1', userId: 'user-1' }])
        .mockResolvedValueOnce([])
    });
    const notifications = createNotificationsUseCase(
      jest.fn().mockResolvedValueOnce(1).mockResolvedValueOnce(0)
    );
    const useCase = new CompleteExpiredReservationsUseCase(
      repository,
      fixedClock('2026-07-05T20:00:00.000Z'),
      notifications
    );

    await expect(useCase.execute()).resolves.toEqual({
      completedReservationsCount: 1,
      reviewPromptNotificationsCreatedCount: 1
    });
    await expect(useCase.execute()).resolves.toEqual({
      completedReservationsCount: 0,
      reviewPromptNotificationsCreatedCount: 0
    });
  });

  it('issues one parameterized SQL update that completes confirmed reservations whose endsAt is exactly now or earlier', async () => {
    const $queryRaw = jest.fn().mockResolvedValue([
      { id: 'reservation-1', userId: 'user-1' },
      { id: 'reservation-2', userId: 'user-2' },
      { id: 'reservation-3', userId: 'user-3' }
    ]);
    const repository = new PrismaReservationRepository({
      $queryRaw,
      $executeRaw: jest.fn(),
      court: {
        findFirst: jest.fn()
      },
      reservation: {
        create: jest.fn(),
        findMany: jest.fn()
      }
    } as never);
    const now = new Date('2026-07-05T20:00:00.000Z');

    await expect(repository.completeExpiredReservations({ now })).resolves.toEqual([
      { id: 'reservation-1', userId: 'user-1' },
      { id: 'reservation-2', userId: 'user-2' },
      { id: 'reservation-3', userId: 'user-3' }
    ]);

    const sql = $queryRaw.mock.calls[0]?.[0] as {
      strings: string[];
      values: unknown[];
    };

    expect(sql.strings.join(' ')).toContain('UPDATE "mejengueros_dev"."Reservation"');
    expect(sql.strings.join(' ')).toContain('"completedAt" = "endsAt"');
    expect(sql.strings.join(' ')).toContain('"status" = CAST(');
    expect(sql.strings.join(' ')).toContain('AND "endsAt" <= ');
    expect(sql.strings.join(' ')).toContain('RETURNING "id", "userId"');
    expect(sql.values).toEqual(['COMPLETED', 'CONFIRMED', now]);
  });
});
