import { plainToInstance } from 'class-transformer';
import { validateSync } from 'class-validator';
import type { PinoLogger } from 'nestjs-pino';
import type { SyncAuthenticatedUserUseCase } from '@/modules/users/application/use-cases/sync-authenticated-user.use-case';
import { CreateReservationUseCase } from '@/modules/reservations/application/use-cases/create-reservation.use-case';
import { GetReservableDaysUseCase } from '@/modules/reservations/application/use-cases/get-reservable-days.use-case';
import { ListMyReservationsUseCase } from '@/modules/reservations/application/use-cases/list-my-reservations.use-case';
import {
  MY_RESERVATIONS_FINALIZED_LIMIT,
  MY_RESERVATIONS_UPCOMING_LIMIT
} from '@/modules/reservations/application/use-cases/list-my-reservations.use-case';
import { GetReservableSlotsUseCase } from '@/modules/reservations/application/use-cases/get-reservable-slots.use-case';
import { StorageInspectionError } from '@/modules/files/infrastructure/errors/storage-inspection.error';
import { PrismaReservationRepository } from '@/modules/reservations/infrastructure/persistence/prisma-reservation.repository';
import { ReservableDaysController } from '@/modules/reservations/interfaces/http/controllers/reservable-days.controller';
import { ReservationsController } from '@/modules/reservations/interfaces/http/controllers/reservations.controller';
import { ReservableSlotsController } from '@/modules/reservations/interfaces/http/controllers/reservable-slots.controller';
import {
  DEFAULT_RESERVABLE_DAYS_RANGE,
  GetReservableDaysRequest,
  MAX_RESERVABLE_DAYS_RANGE
} from '@/modules/reservations/interfaces/http/dto/reservable-days.request';
import type { IClock } from '@/shared/application/clock/clock.port';
import {
  assertReservationStartsWithMinimumAdvance,
  assertCourtCanBeReserved,
  buildReservableSlots,
  parseDateOnly,
  resolveReservationTime
} from '@/modules/reservations/domain/services/reservation-slot-policy';
import type {
  IMyReservationSnapshot,
  IMyReservationsSnapshotGroups,
  IReservationRepository,
  IReservationWindowSnapshot
} from '@/modules/reservations/domain/repositories/reservation.repository';

const COSTA_RICA_UTC_ROLLOVER_INSTANT = '2026-07-05T02:24:00.000Z';
const COSTA_RICA_UTC_ROLLOVER_BUSINESS_DATE = '2026-07-04';
const COSTA_RICA_DAY_WINDOW_START_UTC = '2026-07-04T06:00:00.000Z';
const NEXT_COSTA_RICA_DAY_WINDOW_START_UTC = '2026-07-05T06:00:00.000Z';

describe('reservations module behavior', () => {
  const authenticatedUser = {
    sub: 'player-sub',
    email: 'player@example.test',
    emailVerified: true,
    name: 'Player One',
    pictureUrl: 'https://example.test/player.png',
    provider: 'Google',
    groups: ['players']
  };

  const reservationWindow: IReservationWindowSnapshot = {
    court: {
      id: 'court-id',
      name: 'Cancha 1',
      status: 'ACTIVE',
      complexStatus: 'ACTIVE'
    },
    availability: {
      days: ['WEDNESDAY'],
      startTime: '18:00',
      endTime: '21:00'
    },
    confirmedStartsAt: []
  };

  const myReservations: IMyReservationSnapshot[] = [
    {
      id: 'upcoming-reservation-id',
      complexName: 'Moravia FC',
      courtName: 'Cancha A',
      imageObjectKey: 'uploads/court-image/player-sub/2026/07/court-a.png',
      startsAt: '2026-07-10T18:00:00.000Z',
      endsAt: '2026-07-10T19:00:00.000Z',
      status: 'CONFIRMED',
      completedAt: null,
      reviewId: null
    },
    {
      id: 'completed-pending-review-id',
      complexName: 'Moravia FC',
      courtName: 'Cancha B',
      imageObjectKey: 'uploads/court-image/player-sub/2026/07/court-b.png',
      startsAt: '2026-07-04T18:00:00.000Z',
      endsAt: '2026-07-04T19:00:00.000Z',
      status: 'COMPLETED',
      completedAt: '2026-07-04T19:05:00.000Z',
      reviewId: null
    },
    {
      id: 'completed-reviewed-id',
      complexName: 'Moravia FC',
      courtName: 'Cancha C',
      imageObjectKey: 'uploads/court-image/player-sub/2026/07/court-c.png',
      startsAt: '2026-07-02T18:00:00.000Z',
      endsAt: '2026-07-02T19:00:00.000Z',
      status: 'COMPLETED',
      completedAt: '2026-07-05T20:00:00.000Z',
      reviewId: 'review-id'
    },
    {
      id: 'cancelled-reservation-id',
      complexName: 'Moravia FC',
      courtName: 'Cancha D',
      startsAt: '2026-07-03T18:00:00.000Z',
      endsAt: '2026-07-03T19:00:00.000Z',
      status: 'CANCELLED',
      completedAt: null,
      reviewId: null
    }
  ];

  const fixedClock = (value: string): IClock => ({
    now: () => new Date(value)
  });

  function createSyncAuthenticatedUserUseCase(): SyncAuthenticatedUserUseCase {
    return {
      execute: jest.fn().mockResolvedValue({
        id: 'user-id',
        email: 'player@example.test'
      })
    } as unknown as SyncAuthenticatedUserUseCase;
  }

  function createLogger(): jest.Mocked<PinoLogger> {
    return {
      warn: jest.fn()
    } as unknown as jest.Mocked<PinoLogger>;
  }

  function createListMyReservationsRepository(
    reservations: IMyReservationsSnapshotGroups = {
      upcoming: myReservations.filter((reservation) => reservation.status === 'CONFIRMED'),
      finalized: myReservations.filter(
        (reservation) => reservation.status === 'COMPLETED' && reservation.completedAt != null
      )
    }
  ): jest.Mocked<IReservationRepository> {
    return {
      getReservationWindow: jest.fn(),
      createConfirmedReservation: jest.fn(),
      findMyReservationsByUserId: jest.fn().mockResolvedValue(reservations)
    } as unknown as jest.Mocked<IReservationRepository>;
  }

  function createReservationRepository(
    overrides: Partial<jest.Mocked<IReservationRepository>> = {}
  ): IReservationRepository {
    return {
      getReservationWindow: jest.fn(),
      createConfirmedReservation: jest.fn(),
      findMyReservationsByUserId: jest.fn().mockResolvedValue({
        upcoming: [],
        finalized: []
      }),
      ...overrides
    } as unknown as IReservationRepository;
  }

  it('creates one confirmed reservation after syncing the authenticated user', async () => {
    const repository = createReservationRepository({
      getReservationWindow: jest.fn().mockResolvedValue(reservationWindow),
      createConfirmedReservation: jest.fn().mockResolvedValue({
        id: 'reservation-id',
        userId: 'user-id',
        courtId: 'court-id',
        startsAt: '2026-07-02T00:00:00.000Z',
        endsAt: '2026-07-02T01:00:00.000Z',
        status: 'CONFIRMED'
      })
    });
    const syncAuthenticatedUser = {
      execute: jest.fn().mockResolvedValue({
        id: 'user-id',
        email: 'player@example.test'
      })
    } as unknown as SyncAuthenticatedUserUseCase;
    const useCase = new CreateReservationUseCase(
      repository,
      syncAuthenticatedUser,
      fixedClock('2026-06-30T10:00:00.000Z')
    );

    await expect(
      useCase.execute(authenticatedUser, {
        courtId: 'court-id',
        startsAt: '2026-07-02T00:00:00.000Z'
      })
    ).resolves.toEqual({
      id: 'reservation-id',
      courtId: 'court-id',
      startsAt: '2026-07-02T00:00:00.000Z',
      endsAt: '2026-07-02T01:00:00.000Z',
      status: 'CONFIRMED'
    });
    expect(repository.getReservationWindow).toHaveBeenCalledWith({
      courtId: 'court-id',
      date: '2026-07-01'
    });
    expect(repository.createConfirmedReservation).toHaveBeenCalledWith({
      userId: 'user-id',
      courtId: 'court-id',
      startsAt: new Date('2026-07-02T00:00:00.000Z'),
      endsAt: new Date('2026-07-02T01:00:00.000Z')
    });
    expect(syncAuthenticatedUser.execute).toHaveBeenCalledWith(authenticatedUser);
  });

  it('groups confirmed reservations into upcoming sorted by startsAt ascending', async () => {
    const repository = createListMyReservationsRepository({
      upcoming: [
        myReservations[0],
        {
          ...myReservations[0],
          id: 'upcoming-later-id',
          startsAt: '2026-07-11T18:00:00.000Z',
          endsAt: '2026-07-11T19:00:00.000Z'
        }
      ],
      finalized: []
    });
    const syncAuthenticatedUser = createSyncAuthenticatedUserUseCase();
    const fileReadUrl = {
      createReadUrl: jest.fn().mockResolvedValue('https://read.example.test/court-a.png')
    };
    const useCase = new ListMyReservationsUseCase(
      repository,
      syncAuthenticatedUser,
      fileReadUrl,
      createLogger()
    );

    await expect(useCase.execute(authenticatedUser)).resolves.toMatchObject({
      upcoming: [
        expect.objectContaining({
          id: 'upcoming-reservation-id',
          section: 'UPCOMING',
          status: 'CONFIRMED',
          reviewStatus: 'NOT_APPLICABLE',
          canReview: false,
          hasReview: false
        }),
        expect.objectContaining({
          id: 'upcoming-later-id',
          section: 'UPCOMING'
        })
      ],
      finalized: []
    });
    expect(repository.findMyReservationsByUserId).toHaveBeenCalledWith({
      userId: 'user-id',
      upcomingLimit: MY_RESERVATIONS_UPCOMING_LIMIT,
      finalizedLimit: MY_RESERVATIONS_FINALIZED_LIMIT
    });
  });

  it('groups completed reservations into finalized sorted by completedAt desc then startsAt desc', async () => {
    const repository = createListMyReservationsRepository({
      upcoming: [],
      finalized: [
        myReservations[2],
        myReservations[1],
        {
          ...myReservations[2],
          id: 'completed-older-id',
          startsAt: '2026-07-01T18:00:00.000Z',
          endsAt: '2026-07-01T19:00:00.000Z',
          completedAt: '2026-07-01T19:05:00.000Z',
          reviewId: null
        }
      ]
    });
    const fileReadUrl = {
      createReadUrl: jest.fn().mockResolvedValue('https://read.example.test/court.png')
    };
    const useCase = new ListMyReservationsUseCase(
      repository,
      createSyncAuthenticatedUserUseCase(),
      fileReadUrl,
      createLogger()
    );

    const result = await useCase.execute(authenticatedUser);

    expect(result.finalized.map((reservation) => reservation.id)).toEqual([
      'completed-reviewed-id',
      'completed-pending-review-id',
      'completed-older-id'
    ]);
  });

  it('returns pending review cards with action fields', async () => {
    const fileReadUrl = {
      createReadUrl: jest.fn().mockResolvedValue('https://read.example.test/court-b.png')
    };
    const useCase = new ListMyReservationsUseCase(
      createListMyReservationsRepository({ upcoming: [], finalized: [myReservations[1]] }),
      createSyncAuthenticatedUserUseCase(),
      fileReadUrl,
      createLogger()
    );

    await expect(useCase.execute(authenticatedUser)).resolves.toEqual({
      upcoming: [],
      finalized: [
        {
          id: 'completed-pending-review-id',
          complexName: 'Moravia FC',
          courtName: 'Cancha B',
          imageUrl: 'https://read.example.test/court-b.png',
          startsAt: '2026-07-04T18:00:00.000Z',
          endsAt: '2026-07-04T19:00:00.000Z',
          status: 'COMPLETED',
          section: 'FINALIZED',
          reviewStatus: 'PENDING_REVIEW',
          canReview: true,
          hasReview: false,
          primaryActionKey: 'leave_review',
          primaryActionLabel: 'Dejar reseña'
        }
      ]
    });
  });

  it('returns reviewed cards with indicator fields', async () => {
    const fileReadUrl = {
      createReadUrl: jest.fn().mockResolvedValue('https://read.example.test/court-c.png')
    };
    const useCase = new ListMyReservationsUseCase(
      createListMyReservationsRepository({ upcoming: [], finalized: [myReservations[2]] }),
      createSyncAuthenticatedUserUseCase(),
      fileReadUrl,
      createLogger()
    );

    await expect(useCase.execute(authenticatedUser)).resolves.toEqual({
      upcoming: [],
      finalized: [
        {
          id: 'completed-reviewed-id',
          complexName: 'Moravia FC',
          courtName: 'Cancha C',
          imageUrl: 'https://read.example.test/court-c.png',
          startsAt: '2026-07-02T18:00:00.000Z',
          endsAt: '2026-07-02T19:00:00.000Z',
          status: 'COMPLETED',
          section: 'FINALIZED',
          reviewStatus: 'REVIEWED',
          canReview: false,
          hasReview: true,
          indicatorKey: 'already_reviewed',
          indicatorLabel: 'Ya dejaste tu reseña'
        }
      ]
    });
  });

  it('keeps cards without imageUrl when the reservation has no image object key', async () => {
    const fileReadUrl = {
      createReadUrl: jest.fn()
    };
    const useCase = new ListMyReservationsUseCase(
      createListMyReservationsRepository({
        upcoming: [
          {
            ...myReservations[0],
            imageObjectKey: undefined
          }
        ],
        finalized: []
      }),
      createSyncAuthenticatedUserUseCase(),
      fileReadUrl,
      createLogger()
    );

    await expect(useCase.execute(authenticatedUser)).resolves.toEqual({
      upcoming: [
        expect.objectContaining({
          id: 'upcoming-reservation-id',
          imageUrl: undefined
        })
      ],
      finalized: []
    });
    expect(fileReadUrl.createReadUrl).not.toHaveBeenCalled();
  });

  it('keeps reservation cards when image signing fails by omitting imageUrl', async () => {
    const imageSigningError = new StorageInspectionError(
      'uploads/court-image/player-sub/2026/07/court-b.png',
      new Error('signed URL unavailable')
    );
    const fileReadUrl = {
      createReadUrl: jest.fn().mockRejectedValue(imageSigningError)
    };
    const logger = createLogger();
    const useCase = new ListMyReservationsUseCase(
      createListMyReservationsRepository({ upcoming: [], finalized: [myReservations[1]] }),
      createSyncAuthenticatedUserUseCase(),
      fileReadUrl,
      logger
    );

    await expect(useCase.execute(authenticatedUser)).resolves.toEqual({
      upcoming: [],
      finalized: [
        expect.objectContaining({
          id: 'completed-pending-review-id',
          imageUrl: undefined
        })
      ]
    });
    expect(logger.warn).toHaveBeenCalledWith(
      {
        reservationId: 'completed-pending-review-id',
        errorName: 'Error',
        errorCode: 'EXTERNAL_SERVICE_ERROR'
      },
      'Unable to create reservation card image read URL.'
    );
    expect(logger.warn.mock.calls[0]?.[0]).not.toHaveProperty('error');
    expect(logger.warn.mock.calls[0]?.[0]).not.toHaveProperty('objectKey');
    expect(logger.warn.mock.calls[0]?.[0]).not.toHaveProperty('message');
    expect(logger.warn.mock.calls[0]?.[0]).not.toHaveProperty('stack');
    expect(logger.warn.mock.calls[0]?.[0]).not.toHaveProperty('logContext');
  });

  it('only signs the bounded reservations returned by the repository', async () => {
    const repository = createListMyReservationsRepository({
      upcoming: Array.from({ length: 2 }, (_, index) => ({
        ...myReservations[0],
        id: `upcoming-${index}`,
        imageObjectKey: `uploads/court-image/player-sub/2026/07/upcoming-${index}.png`
      })),
      finalized: Array.from({ length: 3 }, (_, index) => ({
        ...myReservations[1],
        id: `finalized-${index}`,
        imageObjectKey: `uploads/court-image/player-sub/2026/07/finalized-${index}.png`
      }))
    });
    const fileReadUrl = {
      createReadUrl: jest.fn().mockImplementation(async (objectKey: string) => objectKey)
    };
    const useCase = new ListMyReservationsUseCase(
      repository,
      createSyncAuthenticatedUserUseCase(),
      fileReadUrl,
      createLogger()
    );

    const result = await useCase.execute(authenticatedUser);

    expect(result.upcoming).toHaveLength(2);
    expect(result.finalized).toHaveLength(3);
    expect(fileReadUrl.createReadUrl).toHaveBeenCalledTimes(5);
    expect(fileReadUrl.createReadUrl).toHaveBeenNthCalledWith(
      1,
      'uploads/court-image/player-sub/2026/07/upcoming-0.png'
    );
    expect(fileReadUrl.createReadUrl).toHaveBeenNthCalledWith(
      5,
      'uploads/court-image/player-sub/2026/07/finalized-2.png'
    );
  });

  it('rejects same-day reservation creation when the slot already started in UTC', async () => {
    const repository = createReservationRepository();
    const syncAuthenticatedUser = {
      execute: jest.fn().mockResolvedValue({
        id: 'user-id',
        email: 'player@example.test'
      })
    } as unknown as SyncAuthenticatedUserUseCase;
    const useCase = new CreateReservationUseCase(
      repository,
      syncAuthenticatedUser,
      fixedClock('2026-07-02T00:30:00.000Z')
    );

    await expect(
      useCase.execute(authenticatedUser, {
        courtId: 'court-id',
        startsAt: '2026-07-02T00:00:00.000Z'
      })
    ).rejects.toThrow('Reservation start time must be strictly in the future.');
    expect(syncAuthenticatedUser.execute).not.toHaveBeenCalled();
    expect(repository.getReservationWindow).not.toHaveBeenCalled();
    expect(repository.createConfirmedReservation).not.toHaveBeenCalled();
  });

  it('rejects same-day reservation creation when the slot starts exactly at the 30-minute threshold', async () => {
    const repository = createReservationRepository();
    const syncAuthenticatedUser = {
      execute: jest.fn().mockResolvedValue({
        id: 'user-id',
        email: 'player@example.test'
      })
    } as unknown as SyncAuthenticatedUserUseCase;
    const useCase = new CreateReservationUseCase(
      repository,
      syncAuthenticatedUser,
      fixedClock('2026-07-01T23:30:00.000Z')
    );

    await expect(
      useCase.execute(authenticatedUser, {
        courtId: 'court-id',
        startsAt: '2026-07-02T00:00:00.000Z'
      })
    ).rejects.toThrow(
      'Same-day reservation start time must be more than 30 minutes in the future.'
    );
    expect(syncAuthenticatedUser.execute).not.toHaveBeenCalled();
    expect(repository.getReservationWindow).not.toHaveBeenCalled();
    expect(repository.createConfirmedReservation).not.toHaveBeenCalled();
  });

  it('rejects malformed UTC reservation starts before syncing the authenticated user', async () => {
    const repository = createReservationRepository();
    const syncAuthenticatedUser = {
      execute: jest.fn()
    } as unknown as SyncAuthenticatedUserUseCase;
    const useCase = new CreateReservationUseCase(
      repository,
      syncAuthenticatedUser,
      fixedClock('2026-07-01T23:30:00.000Z')
    );

    await expect(
      useCase.execute(authenticatedUser, {
        courtId: 'court-id',
        startsAt: '2026-07-01T18:30:00.000Z'
      })
    ).rejects.toThrow(
      'Reservation start time must be a real UTC ISO datetime with explicit Z aligned to a whole hour.'
    );
    expect(syncAuthenticatedUser.execute).not.toHaveBeenCalled();
    expect(repository.getReservationWindow).not.toHaveBeenCalled();
    expect(repository.createConfirmedReservation).not.toHaveBeenCalled();
  });

  it('allows same-day reservation creation beyond the 30-minute threshold when availability permits it', async () => {
    const repository = createReservationRepository({
      getReservationWindow: jest.fn().mockResolvedValue(reservationWindow),
      createConfirmedReservation: jest.fn().mockResolvedValue({
        id: 'reservation-id',
        userId: 'user-id',
        courtId: 'court-id',
        startsAt: '2026-07-02T00:00:00.000Z',
        endsAt: '2026-07-02T01:00:00.000Z',
        status: 'CONFIRMED'
      })
    });
    const syncAuthenticatedUser = {
      execute: jest.fn().mockResolvedValue({
        id: 'user-id',
        email: 'player@example.test'
      })
    } as unknown as SyncAuthenticatedUserUseCase;
    const useCase = new CreateReservationUseCase(
      repository,
      syncAuthenticatedUser,
      fixedClock('2026-07-01T23:29:00.000Z')
    );

    await expect(
      useCase.execute(authenticatedUser, {
        courtId: 'court-id',
        startsAt: '2026-07-02T00:00:00.000Z'
      })
    ).resolves.toMatchObject({
      id: 'reservation-id',
      courtId: 'court-id',
      startsAt: '2026-07-02T00:00:00.000Z'
    });
    expect(syncAuthenticatedUser.execute).toHaveBeenCalledWith(authenticatedUser);
    expect(repository.createConfirmedReservation).toHaveBeenCalled();
  });

  it('loads reservable slots from the repository window', async () => {
    const repository = createReservationRepository({
      getReservationWindow: jest.fn().mockResolvedValue({
        ...reservationWindow,
        confirmedStartsAt: ['2026-07-02T01:00:00.000Z']
      }),
    });
    const useCase = new GetReservableSlotsUseCase(
      repository,
      fixedClock('2026-06-30T10:00:00.000Z')
    );

    await expect(useCase.execute('court-id', '2026-07-01')).resolves.toEqual({
      court: {
        id: 'court-id',
        name: 'Cancha 1',
        status: 'ACTIVE'
      },
      date: '2026-07-01',
      availabilityStatus: 'AVAILABLE',
      slots: [
        {
          startsAt: '2026-07-02T00:00:00.000Z',
          endsAt: '2026-07-02T01:00:00.000Z'
        },
        {
          startsAt: '2026-07-02T02:00:00.000Z',
          endsAt: '2026-07-02T03:00:00.000Z'
        }
      ]
    });
  });

  it('returns only upcoming dates with at least one available slot', async () => {
    const repository = createReservationRepository({
      getReservationWindow: jest
        .fn()
        .mockResolvedValueOnce({
          ...reservationWindow,
          confirmedStartsAt: ['2026-07-02T01:00:00.000Z']
        })
        .mockResolvedValueOnce({
          ...reservationWindow,
          availability: {
            ...reservationWindow.availability,
            days: ['THURSDAY']
          },
          confirmedStartsAt: [
            '2026-07-03T00:00:00.000Z',
            '2026-07-03T01:00:00.000Z',
            '2026-07-03T02:00:00.000Z'
          ]
        })
        .mockResolvedValueOnce({
          ...reservationWindow,
          availability: {
            ...reservationWindow.availability,
            days: ['SUNDAY']
          }
        })
    });
    const useCase = new GetReservableDaysUseCase(
      repository,
      fixedClock('2026-07-01T10:00:00.000Z')
    );

    await expect(useCase.execute('court-id', '2026-07-01', 3)).resolves.toEqual({
      court: {
        id: 'court-id',
        name: 'Cancha 1',
        status: 'ACTIVE'
      },
      from: '2026-07-01',
      days: 3,
      reservableDays: [
        {
          date: '2026-07-01',
          availabilityStatus: 'AVAILABLE',
          availableSlotsCount: 2
        }
      ]
    });
    expect(repository.getReservationWindow).toHaveBeenNthCalledWith(1, {
      courtId: 'court-id',
      date: '2026-07-01'
    });
    expect(repository.getReservationWindow).toHaveBeenNthCalledWith(2, {
      courtId: 'court-id',
      date: '2026-07-02'
    });
    expect(repository.getReservationWindow).toHaveBeenNthCalledWith(3, {
      courtId: 'court-id',
      date: '2026-07-03'
    });
  });

  it('excludes owner-unconfigured weekdays from reservable day discovery', async () => {
    const repository = createReservationRepository({
      getReservationWindow: jest.fn().mockResolvedValue({
        ...reservationWindow,
        availability: {
          ...reservationWindow.availability,
          days: ['THURSDAY']
        }
      })
    });
    const useCase = new GetReservableDaysUseCase(
      repository,
      fixedClock('2026-07-01T10:00:00.000Z')
    );

    await expect(useCase.execute('court-id', '2026-07-01', 1)).resolves.toEqual({
      court: {
        id: 'court-id',
        name: 'Cancha 1',
        status: 'ACTIVE'
      },
      from: '2026-07-01',
      days: 1,
      reservableDays: []
    });
  });

  it('excludes fully booked days from reservable day discovery', async () => {
    const repository = createReservationRepository({
      getReservationWindow: jest.fn().mockResolvedValue({
        ...reservationWindow,
        confirmedStartsAt: [
          '2026-07-02T00:00:00.000Z',
          '2026-07-02T01:00:00.000Z',
          '2026-07-02T02:00:00.000Z'
        ]
      })
    });
    const useCase = new GetReservableDaysUseCase(
      repository,
      fixedClock('2026-06-30T10:00:00.000Z')
    );

    await expect(useCase.execute('court-id', '2026-07-01', 1)).resolves.toEqual({
      court: {
        id: 'court-id',
        name: 'Cancha 1',
        status: 'ACTIVE'
      },
      from: '2026-07-01',
      days: 1,
      reservableDays: []
    });
  });

  it('excludes same-day slots whose start time is within the 30-minute threshold in UTC', async () => {
    const repository = createReservationRepository({
      getReservationWindow: jest.fn().mockResolvedValue(reservationWindow)
    });
    const useCase = new GetReservableSlotsUseCase(
      repository,
      fixedClock('2026-07-02T00:30:00.000Z')
    );

    await expect(useCase.execute('court-id', '2026-07-01')).resolves.toEqual({
      court: {
        id: 'court-id',
        name: 'Cancha 1',
        status: 'ACTIVE'
      },
      date: '2026-07-01',
      availabilityStatus: 'AVAILABLE',
      slots: [
        {
          startsAt: '2026-07-02T02:00:00.000Z',
          endsAt: '2026-07-02T03:00:00.000Z'
        }
      ]
    });
  });

  it('omits today from reservable day discovery when all same-day slots are inside the 30-minute threshold', async () => {
    const repository = createReservationRepository({
      getReservationWindow: jest
        .fn()
        .mockResolvedValueOnce(reservationWindow)
        .mockResolvedValueOnce({
          ...reservationWindow,
          availability: {
            ...reservationWindow.availability,
            days: ['THURSDAY']
          },
          confirmedStartsAt: ['2026-07-03T02:00:00.000Z']
        })
    });
    const useCase = new GetReservableDaysUseCase(
      repository,
      fixedClock('2026-07-02T01:31:00.000Z')
    );

    await expect(useCase.execute('court-id', '2026-07-01', 2)).resolves.toEqual({
      court: {
        id: 'court-id',
        name: 'Cancha 1',
        status: 'ACTIVE'
      },
      from: '2026-07-01',
      days: 2,
      reservableDays: [
        {
          date: '2026-07-02',
          availabilityStatus: 'AVAILABLE',
          availableSlotsCount: 2
        }
      ]
    });
  });

  it('keeps today in reservable day discovery when a same-day slot remains beyond the 30-minute threshold', async () => {
    const repository = createReservationRepository({
      getReservationWindow: jest.fn().mockResolvedValue(reservationWindow)
    });
    const useCase = new GetReservableDaysUseCase(
      repository,
      fixedClock('2026-07-02T00:31:00.000Z')
    );

    await expect(useCase.execute('court-id', '2026-07-01', 1)).resolves.toEqual({
      court: {
        id: 'court-id',
        name: 'Cancha 1',
        status: 'ACTIVE'
      },
      from: '2026-07-01',
      days: 1,
      reservableDays: [
        {
          date: '2026-07-01',
          availabilityStatus: 'AVAILABLE',
          availableSlotsCount: 1
        }
      ]
    });
  });

  it('delegates the create endpoint to the command use case', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue({ id: 'reservation-id' })
    } as unknown as CreateReservationUseCase;
    const controller = new ReservationsController(useCase, {
      execute: jest.fn()
    } as unknown as ListMyReservationsUseCase);

    await expect(
      controller.create(authenticatedUser, {
        courtId: 'court-id',
        startsAt: '2026-07-02T00:00:00.000Z'
      })
    ).resolves.toEqual({ id: 'reservation-id' });
  });

  it('delegates the my reservations endpoint to the query use case', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue({ upcoming: [], finalized: [] })
    } as unknown as ListMyReservationsUseCase;
    const controller = new ReservationsController({
      execute: jest.fn()
    } as unknown as CreateReservationUseCase, useCase);

    await expect(controller.my(authenticatedUser)).resolves.toEqual({
      upcoming: [],
      finalized: []
    });
    expect(useCase.execute).toHaveBeenCalledWith(authenticatedUser);
  });

  it('delegates the reservable days endpoint to the query use case', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue({ reservableDays: [] })
    } as unknown as GetReservableDaysUseCase;
    const controller = new ReservableDaysController(useCase);

    await expect(
      controller.get('court-id', { from: '2026-07-01', days: 7 })
    ).resolves.toEqual({ reservableDays: [] });
    expect(useCase.execute).toHaveBeenCalledWith('court-id', '2026-07-01', 7);
  });

  it('delegates the reservable slots endpoint to the query use case', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue({ availabilityStatus: 'AVAILABLE' })
    } as unknown as GetReservableSlotsUseCase;
    const controller = new ReservableSlotsController(useCase);

    await expect(controller.get('court-id', { date: '2026-07-01' })).resolves.toEqual({
      availabilityStatus: 'AVAILABLE'
    });
  });

  it('defaults reservable day query days to the safe bounded window', () => {
    const query = plainToInstance(GetReservableDaysRequest, { from: '2026-07-01' });

    expect(query.days).toBe(DEFAULT_RESERVABLE_DAYS_RANGE);
    expect(validateSync(query)).toEqual([]);
  });

  it('rejects invalid reservable day query bounds', () => {
    const query = plainToInstance(GetReservableDaysRequest, {
      from: '2026-07-01',
      days: MAX_RESERVABLE_DAYS_RANGE + 1
    });

    const errors = validateSync(query);

    expect(errors).toHaveLength(1);
    expect(errors[0]?.property).toBe('days');
  });

  it('rejects malformed reservable day start dates', () => {
    const query = plainToInstance(GetReservableDaysRequest, {
      from: '2026-07-32',
      days: 14
    });

    const errors = validateSync(query);

    expect(errors).toHaveLength(1);
    expect(errors[0]?.property).toBe('from');
  });

  it('rejects reservation starts that are not aligned to a whole hour', () => {
    expect(() => resolveReservationTime('2026-07-01T18:30:00.000Z')).toThrow(
      'Reservation start time must be a real UTC ISO datetime with explicit Z aligned to a whole hour.'
    );
  });

  it('rejects reservation starts with timezone offsets even when they land on a whole UTC hour', () => {
    expect(() => resolveReservationTime('2026-07-01T12:00:00-06:00')).toThrow(
      'Reservation start time must be a real UTC ISO datetime with explicit Z aligned to a whole hour.'
    );
  });

  it('rejects reservation starts without an explicit UTC timezone suffix', () => {
    expect(() => resolveReservationTime('2026-07-01T18:00:00')).toThrow(
      'Reservation start time must be a real UTC ISO datetime with explicit Z aligned to a whole hour.'
    );
  });

  it('rejects reservation starts with date-only values', () => {
    expect(() => resolveReservationTime('2026-07-01')).toThrow(
      'Reservation start time must be a real UTC ISO datetime with explicit Z aligned to a whole hour.'
    );
  });

  it('rejects reservations outside configured availability', () => {
    expect(() =>
      assertCourtCanBeReserved(reservationWindow, resolveReservationTime('2026-07-01T21:00:00.000Z'))
    ).toThrow(
      'Reservation start time must fit within the configured one-hour court availability window.'
    );
  });

  it('rejects reservations on dates not enabled by the court weekday configuration', () => {
    expect(() =>
      assertCourtCanBeReserved(reservationWindow, resolveReservationTime('2026-07-03T00:00:00.000Z'))
    ).toThrow('Court is not reservable on the selected date.');
  });

  it('returns unavailable when the selected date falls outside availability days', () => {
    expect(buildReservableSlots(reservationWindow, '2026-07-02')).toEqual({
      availabilityStatus: 'UNAVAILABLE',
      slots: []
    });
  });

  it('returns fully booked when all configured one-hour slots are already confirmed', () => {
    expect(
      buildReservableSlots(
        {
          ...reservationWindow,
          confirmedStartsAt: [
            '2026-07-02T00:00:00.000Z',
            '2026-07-02T01:00:00.000Z',
            '2026-07-02T02:00:00.000Z'
          ]
        },
        '2026-07-01'
      )
    ).toEqual({
      availabilityStatus: 'FULLY_BOOKED',
      slots: []
    });
  });

  it('excludes current-day slots whose UTC start time is less than or equal to the 30-minute threshold', () => {
    expect(
      buildReservableSlots(
        reservationWindow,
        '2026-07-01',
        new Date('2026-07-02T00:30:00.000Z')
      )
    ).toEqual({
      availabilityStatus: 'AVAILABLE',
      slots: [
        {
          startsAt: '2026-07-02T02:00:00.000Z',
          endsAt: '2026-07-02T03:00:00.000Z'
        }
      ]
    });
  });

  it('rejects invalid date-only query values', () => {
    expect(() => parseDateOnly('2026-02-31')).toThrow(
      'Date must use a real YYYY-MM-DD calendar date.'
    );
  });

  it('rejects reservation times whose same-day UTC start does not clear the 30-minute threshold', () => {
    expect(() =>
      assertReservationStartsWithMinimumAdvance(
        resolveReservationTime('2026-07-02T00:00:00.000Z'),
        new Date('2026-07-01T23:30:00.000Z')
      )
    ).toThrow('Same-day reservation start time must be more than 30 minutes in the future.');
  });

  it('allows reservation times that clear the same-day 30-minute threshold', () => {
    expect(() =>
      assertReservationStartsWithMinimumAdvance(
        resolveReservationTime('2026-07-02T00:00:00.000Z'),
        new Date('2026-07-01T23:29:00.000Z')
      )
    ).not.toThrow();
  });

  it('maps prisma unique conflicts to a safe reservation business error', async () => {
    const repository = new PrismaReservationRepository({
      court: {
        findFirst: jest.fn()
      },
      reservation: {
        create: jest.fn().mockRejectedValue({ code: 'P2002' })
      }
    } as never);

    await expect(
      repository.createConfirmedReservation({
        userId: 'user-id',
        courtId: 'court-id',
        startsAt: new Date('2026-07-02T00:00:00.000Z'),
        endsAt: new Date('2026-07-02T01:00:00.000Z')
      })
    ).rejects.toMatchObject({
      code: 'CONFLICT',
      userMessage:
        'This court already has a confirmed reservation for the selected start time.'
    });
  });

  it('reads one reservation window and serializes confirmed slot times', async () => {
    const repository = new PrismaReservationRepository({
      court: {
        findFirst: jest.fn().mockResolvedValue({
          id: 'court-id',
          name: 'Cancha 1',
          status: 'ACTIVE',
          complex: { status: 'ACTIVE' },
          availability: {
            startTime: new Date('1970-01-01T18:00:00.000Z'),
            endTime: new Date('1970-01-01T21:00:00.000Z'),
            days: [{ day: 'WEDNESDAY' }]
          },
          reservations: [{ startsAt: new Date('2026-07-02T01:00:00.000Z') }]
        })
      },
      reservation: {
        create: jest.fn()
      }
    } as never);

    await expect(repository.getReservationWindow({ courtId: 'court-id', date: '2026-07-01' })).resolves.toEqual({
      court: {
        id: 'court-id',
        name: 'Cancha 1',
        status: 'ACTIVE',
        complexStatus: 'ACTIVE'
      },
      availability: {
        days: ['WEDNESDAY'],
        startTime: '18:00',
        endTime: '21:00'
      },
      confirmedStartsAt: ['2026-07-02T01:00:00.000Z']
    });
  });

  it('locks reservation window queries to Costa Rica day bounds', async () => {
    const findFirst = jest.fn().mockResolvedValue({
      id: 'court-id',
      name: 'Cancha 1',
      status: 'ACTIVE',
      complex: { status: 'ACTIVE' },
      availability: {
        startTime: new Date('1970-01-01T18:00:00.000Z'),
        endTime: new Date('1970-01-01T21:00:00.000Z'),
        days: [{ day: 'SATURDAY' }]
      },
      reservations: []
    });
    const repository = new PrismaReservationRepository({
      court: {
        findFirst
      },
      reservation: {
        create: jest.fn()
      }
    } as never);

    await repository.getReservationWindow({
      courtId: 'court-id',
      date: COSTA_RICA_UTC_ROLLOVER_BUSINESS_DATE
    });

    const startsAtBounds = findFirst.mock.calls[0]?.[0]?.select?.reservations?.where?.startsAt;

    expect(startsAtBounds?.gte.toISOString()).toBe(COSTA_RICA_DAY_WINDOW_START_UTC);
    expect(startsAtBounds?.lt.toISOString()).toBe(NEXT_COSTA_RICA_DAY_WINDOW_START_UTC);
  });

  it('filters my reservations persistence reads so cancelled reservations never enter upcoming cards', async () => {
    const findMany = jest
      .fn()
      .mockResolvedValueOnce([
        {
          id: 'upcoming-reservation-id',
          startsAt: new Date('2026-07-10T18:00:00.000Z'),
          endsAt: new Date('2026-07-10T19:00:00.000Z'),
          status: 'CONFIRMED',
          completedAt: null,
          review: null,
          court: {
            name: 'Cancha A',
            imageUpload: { objectKey: 'uploads/court-image/player-sub/2026/07/court-a.png' },
            complex: { name: 'Moravia FC' }
          }
        }
      ])
      .mockResolvedValueOnce([
        {
          id: 'completed-reviewed-id',
          startsAt: new Date('2026-07-04T18:00:00.000Z'),
          endsAt: new Date('2026-07-04T19:00:00.000Z'),
          status: 'COMPLETED',
          completedAt: new Date('2026-07-04T19:05:00.000Z'),
          review: { id: 'review-id' },
          court: {
            name: 'Cancha C',
            imageUpload: { objectKey: 'uploads/court-image/player-sub/2026/07/court-c.png' },
            complex: { name: 'Moravia FC' }
          }
        }
      ]);
    const repository = new PrismaReservationRepository({
      court: {
        findFirst: jest.fn()
      },
      reservation: {
        create: jest.fn(),
        findMany
      }
    } as never);

    await expect(
      repository.findMyReservationsByUserId({
        userId: 'user-id',
        upcomingLimit: 20,
        finalizedLimit: 20
      })
    ).resolves.toEqual({
      upcoming: [
        expect.objectContaining({
          id: 'upcoming-reservation-id',
          status: 'CONFIRMED'
        })
      ],
      finalized: [
        expect.objectContaining({
          id: 'completed-reviewed-id',
          status: 'COMPLETED',
          reviewId: 'review-id'
        })
      ]
    });

    expect(findMany).toHaveBeenNthCalledWith(
      1,
      expect.objectContaining({
        where: {
          userId: 'user-id',
          status: 'CONFIRMED',
          court: {
            deletedAt: null,
            complex: {
              deletedAt: null
            }
          }
        },
        orderBy: [{ startsAt: 'asc' }, { id: 'asc' }],
        take: 20
      })
    );
    expect(findMany.mock.calls[0]?.[0]?.where).not.toHaveProperty('completedAt');
    expect(findMany).toHaveBeenNthCalledWith(
      2,
      expect.objectContaining({
        where: {
          userId: 'user-id',
          status: 'COMPLETED',
          completedAt: {
            not: null
          },
          court: {
            deletedAt: null,
            complex: {
              deletedAt: null
            }
          }
        },
        orderBy: [{ completedAt: 'desc' }, { startsAt: 'desc' }, { id: 'asc' }],
        take: 20
      })
    );
  });

  it('queries only completed reservations with completedAt present for finalized cards', async () => {
    const findMany = jest.fn().mockResolvedValue([]);
    const repository = new PrismaReservationRepository({
      court: {
        findFirst: jest.fn()
      },
      reservation: {
        create: jest.fn(),
        findMany
      }
    } as never);

    await repository.findMyReservationsByUserId({
      userId: 'user-id',
      upcomingLimit: 20,
      finalizedLimit: 20
    });

    expect(findMany.mock.calls[1]?.[0]?.where).toEqual({
      userId: 'user-id',
      status: 'COMPLETED',
      completedAt: {
        not: null
      },
      court: {
        deletedAt: null,
        complex: {
          deletedAt: null
        }
      }
    });
  });

  it('adds stable reservation id tie-breakers to my reservations persistence ordering', async () => {
    const findMany = jest.fn().mockResolvedValue([]);
    const repository = new PrismaReservationRepository({
      court: {
        findFirst: jest.fn()
      },
      reservation: {
        create: jest.fn(),
        findMany
      }
    } as never);

    await repository.findMyReservationsByUserId({
      userId: 'user-id',
      upcomingLimit: 20,
      finalizedLimit: 20
    });

    expect(findMany.mock.calls[0]?.[0]?.orderBy).toEqual([
      { startsAt: 'asc' },
      { id: 'asc' }
    ]);
    expect(findMany.mock.calls[1]?.[0]?.orderBy).toEqual([
      { completedAt: 'desc' },
      { startsAt: 'desc' },
      { id: 'asc' }
    ]);
  });

  it('treats 2026-07-02T02:24:00.000Z as 2026-07-01 in Costa Rica business date logic', () => {
    expect(parseDateOnly('2026-07-01').toISOString()).toBe('2026-07-01T06:00:00.000Z');
    expect(
      buildReservableSlots(
        reservationWindow,
        '2026-07-01',
        new Date('2026-07-02T02:24:00.000Z')
      )
    ).toEqual({
      availabilityStatus: 'FULLY_BOOKED',
      slots: []
    });
  });

  it('treats the fresh review rollover instant as the previous Costa Rica business date', () => {
    expect(parseDateOnly(COSTA_RICA_UTC_ROLLOVER_BUSINESS_DATE).toISOString()).toBe(
      COSTA_RICA_DAY_WINDOW_START_UTC
    );
    expect(
      buildReservableSlots(
        {
          ...reservationWindow,
          availability: {
            days: ['SATURDAY'],
            startTime: '18:00',
            endTime: '21:00'
          }
        },
        COSTA_RICA_UTC_ROLLOVER_BUSINESS_DATE,
        new Date(COSTA_RICA_UTC_ROLLOVER_INSTANT)
      )
    ).toEqual({
      availabilityStatus: 'FULLY_BOOKED',
      slots: []
    });
  });
});
