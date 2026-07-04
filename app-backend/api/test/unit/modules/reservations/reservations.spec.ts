import { plainToInstance } from 'class-transformer';
import { validateSync } from 'class-validator';
import type { SyncAuthenticatedUserUseCase } from '@/modules/users/application/use-cases/sync-authenticated-user.use-case';
import { CreateReservationUseCase } from '@/modules/reservations/application/use-cases/create-reservation.use-case';
import { GetReservableDaysUseCase } from '@/modules/reservations/application/use-cases/get-reservable-days.use-case';
import { GetReservableSlotsUseCase } from '@/modules/reservations/application/use-cases/get-reservable-slots.use-case';
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
  assertReservationStartsInFuture,
  assertCourtCanBeReserved,
  buildReservableSlots,
  parseDateOnly,
  resolveReservationTime
} from '@/modules/reservations/domain/services/reservation-slot-policy';
import type {
  IReservationRepository,
  IReservationWindowSnapshot
} from '@/modules/reservations/domain/repositories/reservation.repository';

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

  const fixedClock = (value: string): IClock => ({
    now: () => new Date(value)
  });

  it('creates one confirmed reservation after syncing the authenticated user', async () => {
    const repository: IReservationRepository = {
      getReservationWindow: jest.fn().mockResolvedValue(reservationWindow),
      createConfirmedReservation: jest.fn().mockResolvedValue({
        id: 'reservation-id',
        userId: 'user-id',
        courtId: 'court-id',
        startsAt: '2026-07-01T18:00:00.000Z',
        endsAt: '2026-07-01T19:00:00.000Z',
        status: 'CONFIRMED'
      })
    };
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
        startsAt: '2026-07-01T18:00:00.000Z'
      })
    ).resolves.toEqual({
      id: 'reservation-id',
      courtId: 'court-id',
      startsAt: '2026-07-01T18:00:00.000Z',
      endsAt: '2026-07-01T19:00:00.000Z',
      status: 'CONFIRMED'
    });
    expect(repository.getReservationWindow).toHaveBeenCalledWith({
      courtId: 'court-id',
      date: '2026-07-01'
    });
    expect(repository.createConfirmedReservation).toHaveBeenCalledWith({
      userId: 'user-id',
      courtId: 'court-id',
      startsAt: new Date('2026-07-01T18:00:00.000Z'),
      endsAt: new Date('2026-07-01T19:00:00.000Z')
    });
    expect(syncAuthenticatedUser.execute).toHaveBeenCalledWith(authenticatedUser);
  });

  it('rejects same-day reservation creation when the slot already started in UTC', async () => {
    const repository: IReservationRepository = {
      getReservationWindow: jest.fn(),
      createConfirmedReservation: jest.fn()
    };
    const syncAuthenticatedUser = {
      execute: jest.fn().mockResolvedValue({
        id: 'user-id',
        email: 'player@example.test'
      })
    } as unknown as SyncAuthenticatedUserUseCase;
    const useCase = new CreateReservationUseCase(
      repository,
      syncAuthenticatedUser,
      fixedClock('2026-07-01T18:30:00.000Z')
    );

    await expect(
      useCase.execute(authenticatedUser, {
        courtId: 'court-id',
        startsAt: '2026-07-01T18:00:00.000Z'
      })
    ).rejects.toThrow('Reservation start time must be strictly in the future.');
    expect(syncAuthenticatedUser.execute).not.toHaveBeenCalled();
    expect(repository.getReservationWindow).not.toHaveBeenCalled();
    expect(repository.createConfirmedReservation).not.toHaveBeenCalled();
  });

  it('rejects same-day reservation creation when the slot starts exactly now in UTC', async () => {
    const repository: IReservationRepository = {
      getReservationWindow: jest.fn(),
      createConfirmedReservation: jest.fn()
    };
    const syncAuthenticatedUser = {
      execute: jest.fn().mockResolvedValue({
        id: 'user-id',
        email: 'player@example.test'
      })
    } as unknown as SyncAuthenticatedUserUseCase;
    const useCase = new CreateReservationUseCase(
      repository,
      syncAuthenticatedUser,
      fixedClock('2026-07-01T18:00:00.000Z')
    );

    await expect(
      useCase.execute(authenticatedUser, {
        courtId: 'court-id',
        startsAt: '2026-07-01T18:00:00.000Z'
      })
    ).rejects.toThrow('Reservation start time must be strictly in the future.');
    expect(syncAuthenticatedUser.execute).not.toHaveBeenCalled();
    expect(repository.getReservationWindow).not.toHaveBeenCalled();
    expect(repository.createConfirmedReservation).not.toHaveBeenCalled();
  });

  it('rejects malformed UTC reservation starts before syncing the authenticated user', async () => {
    const repository: IReservationRepository = {
      getReservationWindow: jest.fn(),
      createConfirmedReservation: jest.fn()
    };
    const syncAuthenticatedUser = {
      execute: jest.fn()
    } as unknown as SyncAuthenticatedUserUseCase;
    const useCase = new CreateReservationUseCase(
      repository,
      syncAuthenticatedUser,
      fixedClock('2026-07-01T17:30:00.000Z')
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

  it('allows same-day future reservation creation when availability permits it', async () => {
    const repository: IReservationRepository = {
      getReservationWindow: jest.fn().mockResolvedValue(reservationWindow),
      createConfirmedReservation: jest.fn().mockResolvedValue({
        id: 'reservation-id',
        userId: 'user-id',
        courtId: 'court-id',
        startsAt: '2026-07-01T18:00:00.000Z',
        endsAt: '2026-07-01T19:00:00.000Z',
        status: 'CONFIRMED'
      })
    };
    const syncAuthenticatedUser = {
      execute: jest.fn().mockResolvedValue({
        id: 'user-id',
        email: 'player@example.test'
      })
    } as unknown as SyncAuthenticatedUserUseCase;
    const useCase = new CreateReservationUseCase(
      repository,
      syncAuthenticatedUser,
      fixedClock('2026-07-01T17:30:00.000Z')
    );

    await expect(
      useCase.execute(authenticatedUser, {
        courtId: 'court-id',
        startsAt: '2026-07-01T18:00:00.000Z'
      })
    ).resolves.toMatchObject({
      id: 'reservation-id',
      courtId: 'court-id',
      startsAt: '2026-07-01T18:00:00.000Z'
    });
    expect(syncAuthenticatedUser.execute).toHaveBeenCalledWith(authenticatedUser);
    expect(repository.createConfirmedReservation).toHaveBeenCalled();
  });

  it('loads reservable slots from the repository window', async () => {
    const repository: IReservationRepository = {
      getReservationWindow: jest.fn().mockResolvedValue({
        ...reservationWindow,
        confirmedStartsAt: ['2026-07-01T19:00:00.000Z']
      }),
      createConfirmedReservation: jest.fn()
    };
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
          startsAt: '2026-07-01T18:00:00.000Z',
          endsAt: '2026-07-01T19:00:00.000Z'
        },
        {
          startsAt: '2026-07-01T20:00:00.000Z',
          endsAt: '2026-07-01T21:00:00.000Z'
        }
      ]
    });
  });

  it('returns only upcoming dates with at least one available slot', async () => {
    const repository: IReservationRepository = {
      getReservationWindow: jest
        .fn()
        .mockResolvedValueOnce({
          ...reservationWindow,
          confirmedStartsAt: ['2026-07-01T19:00:00.000Z']
        })
        .mockResolvedValueOnce({
          ...reservationWindow,
          availability: {
            ...reservationWindow.availability,
            days: ['THURSDAY']
          },
          confirmedStartsAt: [
            '2026-07-02T18:00:00.000Z',
            '2026-07-02T19:00:00.000Z',
            '2026-07-02T20:00:00.000Z'
          ]
        })
        .mockResolvedValueOnce({
          ...reservationWindow,
          availability: {
            ...reservationWindow.availability,
            days: ['SUNDAY']
          }
        }),
      createConfirmedReservation: jest.fn()
    };
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
    const repository: IReservationRepository = {
      getReservationWindow: jest.fn().mockResolvedValue({
        ...reservationWindow,
        availability: {
          ...reservationWindow.availability,
          days: ['THURSDAY']
        }
      }),
      createConfirmedReservation: jest.fn()
    };
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
    const repository: IReservationRepository = {
      getReservationWindow: jest.fn().mockResolvedValue({
        ...reservationWindow,
        confirmedStartsAt: [
          '2026-07-01T18:00:00.000Z',
          '2026-07-01T19:00:00.000Z',
          '2026-07-01T20:00:00.000Z'
        ]
      }),
      createConfirmedReservation: jest.fn()
    };
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

  it('excludes same-day slots whose start time has already passed in UTC', async () => {
    const repository: IReservationRepository = {
      getReservationWindow: jest.fn().mockResolvedValue(reservationWindow),
      createConfirmedReservation: jest.fn()
    };
    const useCase = new GetReservableSlotsUseCase(
      repository,
      fixedClock('2026-07-01T19:30:00.000Z')
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
          startsAt: '2026-07-01T20:00:00.000Z',
          endsAt: '2026-07-01T21:00:00.000Z'
        }
      ]
    });
  });

  it('omits today from reservable day discovery when no future same-day slots remain', async () => {
    const repository: IReservationRepository = {
      getReservationWindow: jest
        .fn()
        .mockResolvedValueOnce(reservationWindow)
        .mockResolvedValueOnce({
          ...reservationWindow,
          availability: {
            ...reservationWindow.availability,
            days: ['THURSDAY']
          },
          confirmedStartsAt: ['2026-07-02T20:00:00.000Z']
        }),
      createConfirmedReservation: jest.fn()
    };
    const useCase = new GetReservableDaysUseCase(
      repository,
      fixedClock('2026-07-01T20:30:00.000Z')
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

  it('delegates the create endpoint to the command use case', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue({ id: 'reservation-id' })
    } as unknown as CreateReservationUseCase;
    const controller = new ReservationsController(useCase);

    await expect(
      controller.create(authenticatedUser, {
        courtId: 'court-id',
        startsAt: '2026-07-01T18:00:00.000Z'
      })
    ).resolves.toEqual({ id: 'reservation-id' });
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
      assertCourtCanBeReserved(reservationWindow, resolveReservationTime('2026-07-02T18:00:00.000Z'))
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
            '2026-07-01T18:00:00.000Z',
            '2026-07-01T19:00:00.000Z',
            '2026-07-01T20:00:00.000Z'
          ]
        },
        '2026-07-01'
      )
    ).toEqual({
      availabilityStatus: 'FULLY_BOOKED',
      slots: []
    });
  });

  it('excludes the current whole-hour slot when its UTC start time is not strictly in the future', () => {
    expect(
      buildReservableSlots(
        reservationWindow,
        '2026-07-01',
        new Date('2026-07-01T18:00:00.000Z')
      )
    ).toEqual({
      availabilityStatus: 'AVAILABLE',
      slots: [
        {
          startsAt: '2026-07-01T19:00:00.000Z',
          endsAt: '2026-07-01T20:00:00.000Z'
        },
        {
          startsAt: '2026-07-01T20:00:00.000Z',
          endsAt: '2026-07-01T21:00:00.000Z'
        }
      ]
    });
  });

  it('rejects invalid date-only query values', () => {
    expect(() => parseDateOnly('2026-02-31')).toThrow(
      'Date must use a real YYYY-MM-DD calendar date.'
    );
  });

  it('rejects reservation times whose UTC start is not strictly in the future', () => {
    expect(() =>
      assertReservationStartsInFuture(
        resolveReservationTime('2026-07-01T18:00:00.000Z'),
        new Date('2026-07-01T18:00:00.000Z')
      )
    ).toThrow('Reservation start time must be strictly in the future.');
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
        startsAt: new Date('2026-07-01T18:00:00.000Z'),
        endsAt: new Date('2026-07-01T19:00:00.000Z')
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
          reservations: [{ startsAt: new Date('2026-07-01T19:00:00.000Z') }]
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
      confirmedStartsAt: ['2026-07-01T19:00:00.000Z']
    });
  });
});
