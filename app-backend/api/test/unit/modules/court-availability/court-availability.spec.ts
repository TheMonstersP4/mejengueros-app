import { GetCourtAvailabilityUseCase } from '@/modules/court-availability/application/use-cases/get-court-availability.use-case';
import { SaveCourtAvailabilityUseCase } from '@/modules/court-availability/application/use-cases/save-court-availability.use-case';
import type {
  ICourtAvailabilityRepository,
  ICourtAvailabilityState
} from '@/modules/court-availability/domain/repositories/court-availability.repository';
import { PrismaCourtAvailabilityRepository } from '@/modules/court-availability/infrastructure/persistence/prisma-court-availability.repository';
import { CourtAvailabilityController } from '@/modules/court-availability/interfaces/http/controllers/court-availability.controller';

describe('court availability module behavior', () => {
  const authenticatedUser = {
    sub: 'owner-sub',
    email: 'owner@example.test',
    emailVerified: true,
    name: 'Owner User',
    pictureUrl: 'https://example.test/owner.png',
    provider: 'Google',
    groups: ['owners']
  };

  const availabilityState: ICourtAvailabilityState = {
    court: {
      id: 'court-id',
      name: 'Cancha 1',
      complexId: 'complex-id',
      complexName: 'Mejengas CR'
    },
    availability: {
      days: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
      startTime: '06:00',
      endTime: '09:00'
    }
  };

  it('loads one owned court availability through the repository port', async () => {
    const repository: ICourtAvailabilityRepository = {
      getOwnedCourtAvailability: jest.fn().mockResolvedValue(availabilityState),
      saveOwnedCourtAvailability: jest.fn()
    };
    const useCase = new GetCourtAvailabilityUseCase(repository);

    await expect(useCase.execute(authenticatedUser, 'court-id')).resolves.toEqual(
      availabilityState
    );
    expect(repository.getOwnedCourtAvailability).toHaveBeenCalledWith({
      ownerIdentity: {
        sub: 'owner-sub',
        provider: 'Google'
      },
      courtId: 'court-id'
    });
  });

  it('saves one owned court availability through the repository port', async () => {
    const repository: ICourtAvailabilityRepository = {
      getOwnedCourtAvailability: jest.fn(),
      saveOwnedCourtAvailability: jest.fn().mockResolvedValue(availabilityState)
    };
    const useCase = new SaveCourtAvailabilityUseCase(repository);

    await expect(
      useCase.execute(authenticatedUser, 'court-id', {
        days: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
        startTime: '06:00',
        endTime: '09:00'
      })
    ).resolves.toEqual(availabilityState);
    expect(repository.saveOwnedCourtAvailability).toHaveBeenCalledWith({
      ownerIdentity: {
        sub: 'owner-sub',
        provider: 'Google'
      },
      courtId: 'court-id',
      availability: {
        days: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
        startTime: '06:00',
        endTime: '09:00'
      }
    });
  });

  it('delegates the read endpoint to the query use case', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue(availabilityState)
    } as unknown as GetCourtAvailabilityUseCase;
    const controller = new CourtAvailabilityController(
      useCase,
      {} as SaveCourtAvailabilityUseCase
    );

    await expect(controller.get(authenticatedUser, 'court-id')).resolves.toEqual(
      availabilityState
    );
    expect(useCase.execute).toHaveBeenCalledWith(authenticatedUser, 'court-id');
  });

  it('delegates the save endpoint to the command use case', async () => {
    const useCase = {
      execute: jest.fn().mockResolvedValue(availabilityState)
    } as unknown as SaveCourtAvailabilityUseCase;
    const controller = new CourtAvailabilityController(
      {} as GetCourtAvailabilityUseCase,
      useCase
    );

    await expect(
      controller.save(authenticatedUser, 'court-id', {
        days: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
        startTime: '06:00',
        endTime: '09:00'
      })
    ).resolves.toEqual(availabilityState);
    expect(useCase.execute).toHaveBeenCalledWith(authenticatedUser, 'court-id', {
      days: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
      startTime: '06:00',
      endTime: '09:00'
    });
  });

  it('creates availability when the owned court has none yet', async () => {
    const harness = createRepositoryHarness();
    const repository = new PrismaCourtAvailabilityRepository(harness.prisma as never);

    await expect(
      repository.saveOwnedCourtAvailability({
        ownerIdentity: { sub: 'owner-sub', provider: 'Google' },
        courtId: 'court-id',
        availability: {
          days: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
          startTime: '06:00',
          endTime: '09:00'
        }
      })
    ).resolves.toEqual(availabilityState);

    expect(harness.state.createdAvailabilityRecords).toEqual([
      {
        courtId: 'court-id',
        startTime: '06:00',
        endTime: '09:00',
        days: ['MONDAY', 'WEDNESDAY', 'FRIDAY']
      }
    ]);
    expect(harness.state.publishedCourtIds).toEqual(['court-id']);
  });

  it('updates days and time range when availability already exists', async () => {
    const harness = createRepositoryHarness({ hasExistingAvailability: true });
    const repository = new PrismaCourtAvailabilityRepository(harness.prisma as never);

    const saved = await repository.saveOwnedCourtAvailability({
      ownerIdentity: { sub: 'owner-sub', provider: 'Google' },
      courtId: 'court-id',
      availability: {
        days: ['TUESDAY', 'THURSDAY'],
        startTime: '08:00',
        endTime: '11:00'
      }
    });

    expect(saved.availability).toEqual({
      days: ['TUESDAY', 'THURSDAY'],
      startTime: '08:00',
      endTime: '11:00'
    });
    expect(harness.state.updatedAvailabilityRecords).toEqual([
      {
        availabilityId: 'availability-id',
        startTime: '08:00',
        endTime: '11:00',
        days: ['TUESDAY', 'THURSDAY']
      }
    ]);
    expect(harness.state.publishedCourtIds).toEqual(['court-id']);
  });

  it('returns null availability when the owned court exists but has not been configured yet', async () => {
    const harness = createRepositoryHarness({ hasExistingAvailability: false });
    const repository = new PrismaCourtAvailabilityRepository(harness.prisma as never);

    await expect(
      repository.getOwnedCourtAvailability({
        ownerIdentity: { sub: 'owner-sub', provider: 'Google' },
        courtId: 'court-id'
      })
    ).resolves.toEqual({
      court: availabilityState.court,
      availability: null
    });
  });

  it('rejects empty weekday selections', async () => {
    const repository = new PrismaCourtAvailabilityRepository(
      createRepositoryHarness().prisma as never
    );

    await expect(
      repository.saveOwnedCourtAvailability({
        ownerIdentity: { sub: 'owner-sub', provider: 'Google' },
        courtId: 'court-id',
        availability: {
          days: [],
          startTime: '06:00',
          endTime: '09:00'
        }
      })
    ).rejects.toMatchObject({
      code: 'VALIDATION_FAILED',
      userMessage: 'Court availability requires at least one weekday.'
    });
  });

  it('rejects time ranges that do not generate exact one-hour slots', async () => {
    const repository = new PrismaCourtAvailabilityRepository(
      createRepositoryHarness().prisma as never
    );

    await expect(
      repository.saveOwnedCourtAvailability({
        ownerIdentity: { sub: 'owner-sub', provider: 'Google' },
        courtId: 'court-id',
        availability: {
          days: ['MONDAY'],
          startTime: '06:30',
          endTime: '09:00'
        }
      })
    ).rejects.toMatchObject({
      code: 'VALIDATION_FAILED',
      userMessage:
        'Court availability must use one shared whole-hour range that produces one-hour slots.'
    });
  });

  it('rejects courts that do not belong to the authenticated owner', async () => {
    const repository = new PrismaCourtAvailabilityRepository(
      createRepositoryHarness({ courtOwnedByRequester: false }).prisma as never
    );

    await expect(
      repository.getOwnedCourtAvailability({
        ownerIdentity: { sub: 'owner-sub', provider: 'Google' },
        courtId: 'court-id'
      })
    ).rejects.toMatchObject({
      code: 'RESOURCE_NOT_FOUND',
      userMessage: 'Court not found for the authenticated owner.'
    });
  });
});

interface IRepositoryHarnessOptions {
  hasExistingAvailability?: boolean;
  courtOwnedByRequester?: boolean;
}

function createRepositoryHarness(options?: IRepositoryHarnessOptions) {
  const state = {
    createdAvailabilityRecords: [] as Array<{
      courtId: string;
      startTime: string;
      endTime: string;
      days: string[];
    }>,
    updatedAvailabilityRecords: [] as Array<{
      availabilityId: string;
      startTime: string;
      endTime: string;
      days: string[];
    }>,
    publishedCourtIds: [] as string[]
  };

  const findOwnedCourt = jest.fn().mockResolvedValue(
    options?.courtOwnedByRequester === false
      ? null
      : {
          id: 'court-id',
          name: 'Cancha 1',
          complexId: 'complex-id',
          complex: { name: 'Mejengas CR' },
          availability:
            options?.hasExistingAvailability === true
              ? {
                  id: 'availability-id',
                  startTime: new Date('1970-01-01T06:00:00.000Z'),
                  endTime: new Date('1970-01-01T09:00:00.000Z'),
                  days: [{ day: 'MONDAY' }, { day: 'WEDNESDAY' }, { day: 'FRIDAY' }]
                }
              : null
        }
  );

  return {
    state,
    prisma: {
      court: {
        findFirst: findOwnedCourt,
        update: jest.fn().mockImplementation(async ({ where }: { where: { id: string } }) => {
          state.publishedCourtIds.push(where.id);
          return { id: where.id };
        })
      },
      courtAvailability: {
        create: jest.fn().mockImplementation(async ({ data }: { data: Record<string, unknown> }) => {
          state.createdAvailabilityRecords.push({
            courtId: String(data.courtId),
            startTime: '06:00',
            endTime: '09:00',
            days: ['MONDAY', 'WEDNESDAY', 'FRIDAY']
          });
          return { id: 'availability-id' };
        }),
        update: jest.fn().mockImplementation(async ({ where, data }: { where: { courtId: string }; data: Record<string, unknown> }) => {
          const createdDays = ((data.days as { create: Array<{ day: string }> }).create).map(
            (entry) => entry.day
          );
          state.updatedAvailabilityRecords.push({
            availabilityId: where.courtId === 'court-id' ? 'availability-id' : 'unknown',
            startTime: '08:00',
            endTime: '11:00',
            days: createdDays
          });
          return { id: 'availability-id' };
        })
      }
    }
  };
}
