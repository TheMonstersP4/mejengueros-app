import type { PinoLogger } from 'nestjs-pino';
import type { IFileReadUrlPort } from '@/modules/files/application/ports/file-read-url.port';
import {
  ListOwnerReservationsUseCase,
  OWNER_RESERVATIONS_FINALIZED_LIMIT,
  OWNER_RESERVATIONS_UPCOMING_LIMIT
} from '@/modules/reservations/application/use-cases/list-owner-reservations.use-case';
import type {
  IOwnerReservationsSnapshotGroups,
  IReservationRepository
} from '@/modules/reservations/domain/repositories/reservation.repository';

describe('ListOwnerReservationsUseCase', () => {
  const owner = {
    sub: 'owner-sub',
    email: 'owner@example.test',
    emailVerified: true,
    name: 'Owner One',
    pictureUrl: 'https://example.test/owner.png',
    provider: 'Cognito',
    groups: ['owners']
  };

  const ownerReservations: IOwnerReservationsSnapshotGroups = {
    upcoming: [
      {
        id: 'upcoming-id',
        complexName: 'Moravia FC',
        courtName: 'Cancha A',
        imageObjectKey: 'uploads/court-a.png',
        startsAt: '2026-07-10T18:00:00.000Z',
        endsAt: '2026-07-10T19:00:00.000Z',
        status: 'CONFIRMED'
      }
    ],
    finalized: [
      {
        id: 'finalized-id',
        complexName: 'Moravia FC',
        courtName: 'Cancha B',
        startsAt: '2026-07-01T18:00:00.000Z',
        endsAt: '2026-07-01T19:00:00.000Z',
        status: 'COMPLETED'
      }
    ]
  };

  function createLogger(): jest.Mocked<PinoLogger> {
    return { warn: jest.fn() } as unknown as jest.Mocked<PinoLogger>;
  }

  function createRepository(
    reservations: IOwnerReservationsSnapshotGroups = ownerReservations
  ): jest.Mocked<IReservationRepository> {
    return {
      getReservationWindow: jest.fn(),
      createConfirmedReservation: jest.fn(),
      findMyReservationsByUserId: jest.fn(),
      listOwnerReservations: jest.fn().mockResolvedValue(reservations),
      completeExpiredReservations: jest.fn()
    } as unknown as jest.Mocked<IReservationRepository>;
  }

  function createFileReadUrl(
    impl: IFileReadUrlPort['createReadUrl'] = jest
      .fn()
      .mockResolvedValue('https://read.example.test/court.png')
  ): jest.Mocked<IFileReadUrlPort> {
    return { createReadUrl: impl } as unknown as jest.Mocked<IFileReadUrlPort>;
  }

  it('groups reservations into upcoming (CONFIRMED) and finalized (COMPLETED) cards with signed images', async () => {
    const repository = createRepository();
    const fileReadUrl = createFileReadUrl();
    const useCase = new ListOwnerReservationsUseCase(repository, fileReadUrl, createLogger());

    const result = await useCase.execute(owner, {});

    expect(repository.listOwnerReservations).toHaveBeenCalledWith({
      ownerIdentity: { sub: 'owner-sub', provider: 'Cognito' },
      upcomingLimit: OWNER_RESERVATIONS_UPCOMING_LIMIT,
      finalizedLimit: OWNER_RESERVATIONS_FINALIZED_LIMIT
    });
    expect(result.selectedCourtId).toBeNull();
    expect(result.upcoming).toEqual([
      {
        id: 'upcoming-id',
        complexName: 'Moravia FC',
        courtName: 'Cancha A',
        imageUrl: 'https://read.example.test/court.png',
        startsAt: '2026-07-10T18:00:00.000Z',
        endsAt: '2026-07-10T19:00:00.000Z',
        status: 'CONFIRMED',
        section: 'UPCOMING'
      }
    ]);
    expect(result.finalized).toEqual([
      {
        id: 'finalized-id',
        complexName: 'Moravia FC',
        courtName: 'Cancha B',
        imageUrl: undefined,
        startsAt: '2026-07-01T18:00:00.000Z',
        endsAt: '2026-07-01T19:00:00.000Z',
        status: 'COMPLETED',
        section: 'FINALIZED'
      }
    ]);
  });

  it('forwards the court filter to the repository and echoes it as selectedCourtId', async () => {
    const repository = createRepository({ upcoming: [], finalized: [] });
    const useCase = new ListOwnerReservationsUseCase(
      repository,
      createFileReadUrl(),
      createLogger()
    );

    const result = await useCase.execute(owner, { courtId: 'court-123' });

    expect(repository.listOwnerReservations).toHaveBeenCalledWith({
      ownerIdentity: { sub: 'owner-sub', provider: 'Cognito' },
      court: { courtId: 'court-123' },
      upcomingLimit: OWNER_RESERVATIONS_UPCOMING_LIMIT,
      finalizedLimit: OWNER_RESERVATIONS_FINALIZED_LIMIT
    });
    expect(result.selectedCourtId).toBe('court-123');
  });

  it('keeps cards without imageUrl when image signing fails', async () => {
    const repository = createRepository({
      upcoming: ownerReservations.upcoming,
      finalized: []
    });
    const fileReadUrl = createFileReadUrl(
      jest.fn().mockRejectedValue(new Error('signing failed'))
    );
    const logger = createLogger();
    const useCase = new ListOwnerReservationsUseCase(repository, fileReadUrl, logger);

    const result = await useCase.execute(owner, {});

    expect(result.upcoming[0]?.imageUrl).toBeUndefined();
    expect(logger.warn).toHaveBeenCalledTimes(1);
  });
});
