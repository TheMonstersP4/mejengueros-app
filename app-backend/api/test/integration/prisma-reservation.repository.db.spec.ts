import { randomUUID } from 'node:crypto';
import { PrismaReservationRepository } from '@/modules/reservations/infrastructure/persistence/prisma-reservation.repository';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';

const RUN_DB_TESTS_ENV = 'RUN_PRISMA_RESERVATION_REPOSITORY_DB_TESTS';
const TEST_DATABASE_URL_ENV = 'PRISMA_RESERVATION_REPOSITORY_DB_TEST_DATABASE_URL';
const controlledDatabaseUrl = process.env[TEST_DATABASE_URL_ENV] ?? process.env.DATABASE_URL;
const originalDatabaseUrl = process.env.DATABASE_URL;
const shouldRunLiveDatabaseIntegration = process.env[RUN_DB_TESTS_ENV] === 'true';

if (shouldRunLiveDatabaseIntegration && !isControlledTestDatabaseUrl(controlledDatabaseUrl)) {
  throw new Error(
    `${RUN_DB_TESTS_ENV}=true requires ${TEST_DATABASE_URL_ENV} (or DATABASE_URL) to point at a dedicated test/ci database.`
  );
}

const runLiveDatabaseIntegration =
  shouldRunLiveDatabaseIntegration && typeof controlledDatabaseUrl === 'string';

(runLiveDatabaseIntegration ? describe : describe.skip)(
  'PrismaReservationRepository live DB integration',
  () => {
    let prismaService: PrismaService;

    beforeAll(async () => {
      process.env.DATABASE_URL = controlledDatabaseUrl;
      prismaService = new PrismaService();
      await prismaService.onModuleInit();
    });

    afterAll(async () => {
      await prismaService?.onModuleDestroy();

      if (originalDatabaseUrl === undefined) {
        delete process.env.DATABASE_URL;
      } else {
        process.env.DATABASE_URL = originalDatabaseUrl;
      }
    });

    it('completes expired confirmed reservations including the endsAt equals now boundary and stays idempotent on rerun', async () => {
      const fixture = await seedReservationCompletionFixture(prismaService);
      const repository = new PrismaReservationRepository(prismaService as never);

      try {
        await expect(
          repository.completeExpiredReservations({
            now: new Date('2026-07-05T20:00:00.000Z')
          })
        ).resolves.toEqual(
          expect.arrayContaining([
            { id: fixture.expiredReservationId, userId: fixture.playerId },
            { id: fixture.endsAtNowReservationId, userId: fixture.playerId }
          ])
        );

        await expect(
          prismaService.reservation.findUnique({
            where: { id: fixture.expiredReservationId },
            select: { status: true, completedAt: true, endsAt: true }
          })
        ).resolves.toEqual({
          status: 'COMPLETED',
          completedAt: new Date('2026-07-05T19:00:00.000Z'),
          endsAt: new Date('2026-07-05T19:00:00.000Z')
        });

        await expect(
          prismaService.reservation.findUnique({
            where: { id: fixture.futureReservationId },
            select: { status: true, completedAt: true }
          })
        ).resolves.toEqual({
          status: 'CONFIRMED',
          completedAt: null
        });

        await expect(
          prismaService.reservation.findUnique({
            where: { id: fixture.endsAtNowReservationId },
            select: { status: true, completedAt: true, endsAt: true }
          })
        ).resolves.toEqual({
          status: 'COMPLETED',
          completedAt: new Date('2026-07-05T20:00:00.000Z'),
          endsAt: new Date('2026-07-05T20:00:00.000Z')
        });

        await expect(
          prismaService.reservation.findUnique({
            where: { id: fixture.completedReservationId },
            select: { status: true, completedAt: true }
          })
        ).resolves.toEqual({
          status: 'COMPLETED',
          completedAt: new Date('2026-07-05T17:00:00.000Z')
        });

        await expect(
          repository.completeExpiredReservations({
            now: new Date('2026-07-05T20:00:00.000Z')
          })
        ).resolves.toEqual([]);
      } finally {
        await cleanupReservationCompletionFixture(prismaService, fixture);
      }
    });
  }
);

interface IReservationCompletionFixture {
  ownerId: string;
  playerId: string;
  complexId: string;
  courtId: string;
  expiredReservationId: string;
  futureReservationId: string;
  endsAtNowReservationId: string;
  completedReservationId: string;
}

async function seedReservationCompletionFixture(
  prisma: PrismaService
): Promise<IReservationCompletionFixture> {
  const ownerId = randomUUID();
  const playerId = randomUUID();
  const complexId = randomUUID();
  const courtId = randomUUID();
  const expiredReservationId = randomUUID();
  const futureReservationId = randomUUID();
  const endsAtNowReservationId = randomUUID();
  const completedReservationId = randomUUID();

  await prisma.user.createMany({
    data: [
      {
        id: ownerId,
        email: `owner-${ownerId}@example.test`
      },
      {
        id: playerId,
        email: `player-${playerId}@example.test`
      }
    ]
  });

  await prisma.complex.create({
    data: {
      id: complexId,
      ownerId,
      name: `Complex ${complexId}`,
      address: 'San Jose, Costa Rica'
    }
  });

  await prisma.court.create({
    data: {
      id: courtId,
      complexId,
      name: `Court ${courtId}`
    }
  });

  await prisma.reservation.createMany({
    data: [
      {
        id: expiredReservationId,
        userId: playerId,
        courtId,
        startsAt: new Date('2026-07-05T18:00:00.000Z'),
        endsAt: new Date('2026-07-05T19:00:00.000Z'),
        status: 'CONFIRMED'
      },
      {
        id: futureReservationId,
        userId: playerId,
        courtId,
        startsAt: new Date('2026-07-05T21:00:00.000Z'),
        endsAt: new Date('2026-07-05T22:00:00.000Z'),
        status: 'CONFIRMED'
      },
      {
        id: endsAtNowReservationId,
        userId: playerId,
        courtId,
        startsAt: new Date('2026-07-05T19:00:00.000Z'),
        endsAt: new Date('2026-07-05T20:00:00.000Z'),
        status: 'CONFIRMED'
      },
      {
        id: completedReservationId,
        userId: playerId,
        courtId,
        startsAt: new Date('2026-07-05T16:00:00.000Z'),
        endsAt: new Date('2026-07-05T17:00:00.000Z'),
        status: 'COMPLETED',
        completedAt: new Date('2026-07-05T17:00:00.000Z')
      }
    ]
  });

  return {
    ownerId,
    playerId,
    complexId,
    courtId,
    expiredReservationId,
    futureReservationId,
    endsAtNowReservationId,
    completedReservationId
  };
}

async function cleanupReservationCompletionFixture(
  prisma: PrismaService,
  fixture: IReservationCompletionFixture
): Promise<void> {
  await prisma.reservation.deleteMany({
    where: {
      id: {
        in: [
          fixture.expiredReservationId,
          fixture.futureReservationId,
          fixture.endsAtNowReservationId,
          fixture.completedReservationId
        ]
      }
    }
  });
  await prisma.court.delete({ where: { id: fixture.courtId } });
  await prisma.complex.delete({ where: { id: fixture.complexId } });
  await prisma.user.deleteMany({
    where: {
      id: {
        in: [fixture.ownerId, fixture.playerId]
      }
    }
  });
}

function isControlledTestDatabaseUrl(databaseUrl: string | undefined): boolean {
  if (typeof databaseUrl !== 'string' || databaseUrl.length === 0) {
    return false;
  }

  try {
    const connectionUrl = new URL(databaseUrl);
    const databaseName = connectionUrl.pathname.replace(/^\//, '');
    const schemaName = connectionUrl.searchParams.get('schema') ?? '';

    return /(?:test|ci)/i.test(databaseName) || /(?:test|ci)/i.test(schemaName);
  } catch {
    return false;
  }
}
