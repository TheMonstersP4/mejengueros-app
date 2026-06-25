import type { PrismaClient } from '../../../src/generated/prisma/client';

import {
  assertSeedEnvironment,
  ensureCantonCatalog,
  ensureProvinceCatalog,
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
          .mockResolvedValueOnce({ id: 'province-by-name', code: 'SJ-OLD', name: 'San Jose' })
      }
    } as unknown as PrismaClient;

    await expect(ensureProvinceCatalog(prisma)).rejects.toThrow(
      'Province seed conflict: code SJ and name San Jose belong to different rows'
    );
  });

  it('fails fast when canton code exists under another province', async () => {
    const prisma = {
      canton: {
        findUnique: jest.fn().mockResolvedValue({
          id: 'canton-by-code',
          provinceId: 'other-province',
          code: 'SJ-01',
          name: 'San Jose'
        }),
        findFirst: jest.fn().mockResolvedValue(null)
      }
    } as unknown as PrismaClient;

    await expect(ensureCantonCatalog(prisma, 'expected-province')).rejects.toThrow(
      'Canton seed conflict: code SJ-01 belongs to province other-province, expected expected-province.'
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
