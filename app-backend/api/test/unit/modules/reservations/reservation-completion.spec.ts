import { CompleteExpiredReservationsUseCase } from '@/modules/reservations/application/use-cases/complete-expired-reservations.use-case';
import type { IClock } from '@/shared/application/clock/clock.port';
import type { IReservationRepository } from '@/modules/reservations/domain/repositories/reservation.repository';
import { PrismaReservationRepository } from '@/modules/reservations/infrastructure/persistence/prisma-reservation.repository';

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
      completeExpiredReservations: jest.fn().mockResolvedValue(0),
      ...overrides
    } as unknown as jest.Mocked<IReservationRepository>;
  }

  it('completes expired confirmed reservations using the current clock time', async () => {
    const repository = createReservationRepository({
      completeExpiredReservations: jest.fn().mockResolvedValue(2)
    });
    const useCase = new CompleteExpiredReservationsUseCase(
      repository,
      fixedClock('2026-07-05T20:00:00.000Z')
    );

    await expect(useCase.execute()).resolves.toBe(2);
    expect(repository.completeExpiredReservations).toHaveBeenCalledWith({
      now: new Date('2026-07-05T20:00:00.000Z')
    });
  });

  it('leaves future confirmed reservations untouched when no rows have expired yet', async () => {
    const repository = createReservationRepository({
      completeExpiredReservations: jest.fn().mockResolvedValue(0)
    });
    const useCase = new CompleteExpiredReservationsUseCase(
      repository,
      fixedClock('2026-07-05T18:59:59.000Z')
    );

    await expect(useCase.execute()).resolves.toBe(0);
  });

  it('is safe under duplicate invocations because reruns can complete zero additional rows', async () => {
    const repository = createReservationRepository({
      completeExpiredReservations: jest.fn().mockResolvedValueOnce(1).mockResolvedValueOnce(0)
    });
    const useCase = new CompleteExpiredReservationsUseCase(
      repository,
      fixedClock('2026-07-05T20:00:00.000Z')
    );

    await expect(useCase.execute()).resolves.toBe(1);
    await expect(useCase.execute()).resolves.toBe(0);
  });

  it('issues one parameterized SQL update that completes confirmed reservations whose endsAt is exactly now or earlier', async () => {
    const $executeRaw = jest.fn().mockResolvedValue(3);
    const repository = new PrismaReservationRepository({
      $executeRaw,
      court: {
        findFirst: jest.fn()
      },
      reservation: {
        create: jest.fn(),
        findMany: jest.fn()
      }
    } as never);
    const now = new Date('2026-07-05T20:00:00.000Z');

    await expect(repository.completeExpiredReservations({ now })).resolves.toBe(3);

    const sql = $executeRaw.mock.calls[0]?.[0] as {
      strings: string[];
      values: unknown[];
    };

    expect(sql.strings.join(' ')).toContain('UPDATE "mejengueros_dev"."Reservation"');
    expect(sql.strings.join(' ')).toContain('"completedAt" = "endsAt"');
    expect(sql.strings.join(' ')).toContain('"status" = CAST(');
    expect(sql.strings.join(' ')).toContain('AND "endsAt" <= ');
    expect(sql.values).toEqual(['COMPLETED', 'CONFIRMED', now]);
  });
});
