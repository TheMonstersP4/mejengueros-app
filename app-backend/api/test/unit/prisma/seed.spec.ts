import type { PrismaClient } from '../../../src/generated/prisma/client';

import {
  assertSeedEnvironment,
  ensureCantonCatalog,
  ensureProvinceCatalog,
  seed,
  seedSharedCatalogs,
  teardown
} from '../../../prisma/seed';

describe('prisma seed safeguards', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('rejects shared-dev GitHub Actions override outside the mejengueros_dev schema', () => {
    expect(() =>
      assertSeedEnvironment({
        ALLOW_DEMO_SEED: 'true',
        NODE_ENV: 'development',
        GITHUB_ACTIONS: 'true',
        ALLOW_SHARED_DEV_DEMO_SEED: 'true',
        DATABASE_URL: 'postgresql://seed-user:secret@shared.example.test:5432/appdb?schema=public'
      })
    ).toThrow('requires DATABASE_URL to use schema=mejengueros_dev');
  });

  it('tears down reservations, notifications, and reviews for demo users or demo courts only', async () => {
    const prisma = {
      userIdentity: {
        findMany: jest.fn().mockResolvedValue([{ userId: 'provider-demo-user' }])
      },
      user: {
        findMany: jest.fn().mockResolvedValue([{ id: 'email-demo-user' }]),
        deleteMany: jest.fn().mockResolvedValue({ count: 2 })
      },
      complex: {
        findMany: jest.fn().mockResolvedValue([{ id: 'demo-complex' }]),
        deleteMany: jest.fn().mockResolvedValue({ count: 1 })
      },
      court: {
        findMany: jest.fn().mockResolvedValue([{ id: 'demo-court' }]),
        deleteMany: jest.fn().mockResolvedValue({ count: 1 })
      },
      reservation: {
        findMany: jest
          .fn()
          .mockResolvedValue([{ id: 'demo-reservation-1' }, { id: 'demo-reservation-2' }]),
        deleteMany: jest.fn().mockResolvedValue({ count: 2 })
      },
      review: {
        deleteMany: jest.fn().mockResolvedValue({ count: 1 })
      },
      notification: {
        deleteMany: jest.fn().mockResolvedValue({ count: 3 })
      }
    } as unknown as PrismaClient;

    await teardown(prisma);

    expect(prisma.reservation.findMany).toHaveBeenCalledWith({
      where: {
        OR: [
          { courtId: { in: ['demo-court'] } },
          { userId: { in: ['provider-demo-user', 'email-demo-user'] } }
        ]
      },
      select: { id: true }
    });
    expect(prisma.review.deleteMany).toHaveBeenCalledWith({
      where: { reservationId: { in: ['demo-reservation-1', 'demo-reservation-2'] } }
    });
    expect(prisma.notification.deleteMany).toHaveBeenCalledWith({
      where: {
        OR: [
          { reservationId: { in: ['demo-reservation-1', 'demo-reservation-2'] } },
          { userId: { in: ['provider-demo-user', 'email-demo-user'] } }
        ]
      }
    });
    expect(prisma.reservation.deleteMany).toHaveBeenCalledWith({
      where: { id: { in: ['demo-reservation-1', 'demo-reservation-2'] } }
    });
  });

  it('fails fast when province code and name point to different rows', async () => {
    const prisma = {
      province: {
        findUnique: jest
          .fn()
          .mockResolvedValueOnce({ id: 'province-by-code', code: 'SJ', name: 'Legacy San Jose' })
          .mockResolvedValueOnce({ id: 'province-by-name', code: 'SJ-OLD', name: 'San José' })
      }
    } as unknown as PrismaClient;

    await expect(ensureProvinceCatalog(prisma)).rejects.toThrow(
      'Province seed conflict: code SJ and name San José belong to different rows'
    );
  });

  it('fails fast when canton code exists under another province', async () => {
    const prisma = {
      canton: {
        findUnique: jest.fn().mockResolvedValue({
          id: 'canton-by-code',
          provinceId: 'other-province',
          code: 'SJ-01',
          name: 'San José'
        }),
        findFirst: jest.fn().mockResolvedValue(null)
      }
    } as unknown as PrismaClient;

    await expect(
      ensureCantonCatalog(prisma, {
        SJ: { id: 'expected-province', code: 'SJ', name: 'San José' }
      })
    ).rejects.toThrow(
      'Canton seed conflict: code SJ-01 belongs to province other-province, expected expected-province.'
    );
  });

  it('reconciles all seven Costa Rica provinces and returns them keyed by code', async () => {
    const create = jest.fn().mockImplementation(async ({ data }) => ({ id: `province-${data.code}`, ...data }));
    const prisma = {
      province: {
        findUnique: jest.fn().mockResolvedValue(null),
        create,
        update: jest.fn()
      }
    } as unknown as PrismaClient;

    const provincesByCode = await ensureProvinceCatalog(prisma);

    expect(Object.keys(provincesByCode)).toHaveLength(7);
    expect(Object.keys(provincesByCode)).toEqual(['SJ', 'AL', 'CA', 'HE', 'GU', 'PU', 'LI']);
    expect(provincesByCode.SJ).toEqual({ id: 'province-SJ', code: 'SJ', name: 'San José' });
    expect(provincesByCode.AL).toEqual({ id: 'province-AL', code: 'AL', name: 'Alajuela' });
    expect(prisma.province.update).not.toHaveBeenCalled();
  });

  it('reconciles all 84 cantons and keeps San José available for the demo complex', async () => {
    const create = jest.fn().mockImplementation(async ({ data }) => ({ id: `canton-${data.code}`, ...data }));
    const prisma = {
      canton: {
        findUnique: jest.fn().mockResolvedValue(null),
        findFirst: jest.fn().mockResolvedValue(null),
        create,
        update: jest.fn()
      }
    } as unknown as PrismaClient;

    const cantonsByCode = await ensureCantonCatalog(prisma, {
      SJ: { id: 'province-SJ', code: 'SJ', name: 'San José' },
      AL: { id: 'province-AL', code: 'AL', name: 'Alajuela' },
      CA: { id: 'province-CA', code: 'CA', name: 'Cartago' },
      HE: { id: 'province-HE', code: 'HE', name: 'Heredia' },
      GU: { id: 'province-GU', code: 'GU', name: 'Guanacaste' },
      PU: { id: 'province-PU', code: 'PU', name: 'Puntarenas' },
      LI: { id: 'province-LI', code: 'LI', name: 'Limón' }
    });

    expect(Object.keys(cantonsByCode)).toHaveLength(84);
    expect(cantonsByCode['SJ-01']).toEqual({
      id: 'canton-SJ-01',
      provinceId: 'province-SJ',
      code: 'SJ-01',
      name: 'San José'
    });
    expect(cantonsByCode['LI-06']).toEqual({
      id: 'canton-LI-06',
      provinceId: 'province-LI',
      code: 'LI-06',
      name: 'Guácimo'
    });
    expect(prisma.canton.update).not.toHaveBeenCalled();
  });

  it('uses the San José province and canton from the full catalog when creating the demo complex', async () => {
    const courtCreate = jest
      .fn()
      .mockResolvedValueOnce({ id: 'court-id' })
      .mockImplementation(async () => ({ id: `demo-court-${courtCreate.mock.calls.length - 1}` }));
    const reservationCreate = jest
      .fn()
      .mockImplementation(async () => ({ id: `reservation-${reservationCreate.mock.calls.length}` }));
    const prisma = {
      province: {
        findUnique: jest.fn().mockResolvedValue(null),
        create: jest.fn().mockImplementation(async ({ data }) => ({ id: `province-${data.code}`, ...data })),
        update: jest.fn()
      },
      canton: {
        findUnique: jest.fn().mockResolvedValue(null),
        findFirst: jest.fn().mockResolvedValue(null),
        create: jest.fn().mockImplementation(async ({ data }) => ({ id: `canton-${data.code}`, ...data })),
        update: jest.fn()
      },
      serviceCatalog: {
        upsert: jest.fn().mockImplementation(async ({ where, create }) => ({ id: `service-${where.name}`, ...create }))
      },
      user: {
        create: jest
          .fn()
          .mockResolvedValueOnce({ id: 'owner-id' })
          .mockResolvedValueOnce({ id: 'player-1-id' })
          .mockResolvedValueOnce({ id: 'player-2-id' })
      },
      complex: {
        create: jest.fn().mockResolvedValue({ id: 'complex-id' })
      },
      court: {
        create: courtCreate
      },
      reservation: {
        create: reservationCreate
      },
      review: {
        create: jest.fn().mockResolvedValue({ id: 'review-id' })
      }
    } as unknown as PrismaClient;

    await seed(prisma);

    expect(prisma.complex.create).toHaveBeenCalledWith(
      expect.objectContaining({
        data: expect.objectContaining({
          provinceId: 'province-SJ',
          cantonId: 'canton-SJ-01'
        })
      })
    );
  });

  it('reconciles shared service catalog scope and active state on every run', async () => {
    const upsert = jest.fn().mockResolvedValue({ id: 'service-id' });
    const prisma = {
      serviceCatalog: { upsert }
    } as unknown as PrismaClient;

    await seedSharedCatalogs(prisma);

    expect(upsert).toHaveBeenNthCalledWith(1, {
      where: { name: 'Parqueo' },
      create: { name: 'Parqueo', scope: 'COMPLEX', isActive: true },
      update: { scope: 'COMPLEX', isActive: true }
    });
    expect(upsert).toHaveBeenNthCalledWith(2, {
      where: { name: 'Iluminacion' },
      create: { name: 'Iluminacion', scope: 'COURT', isActive: true },
      update: { scope: 'COURT', isActive: true }
    });
    expect(upsert).toHaveBeenCalledTimes(5);
  });
});
