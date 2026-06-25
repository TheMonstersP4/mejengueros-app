import 'dotenv/config';
import { PrismaPg } from '@prisma/adapter-pg';
import { Pool } from 'pg';
import { PrismaClient } from '../src/generated/prisma/client';

type ProvinceSeedRecord = {
  id: string;
  code: string;
  name: string;
};

type CantonSeedRecord = {
  id: string;
  provinceId: string;
  code: string;
  name: string;
};

type SeedEnvironment = NodeJS.ProcessEnv;

type ServiceCatalogSeed = {
  name: string;
  scope: 'COMPLEX' | 'COURT';
};

function assertLocalDatabase(rawUrl: string, env: SeedEnvironment = process.env): void {
  let parsed: URL;
  try {
    parsed = new URL(rawUrl);
  } catch {
    throw new Error('ERROR: DATABASE_URL is not a valid URL.');
  }

  const host = parsed.hostname;
  const dbName = parsed.pathname.replace(/^\//, '');
  const schema = parsed.searchParams.get('schema');

  const isLocalHost = host === 'localhost' || host === '127.0.0.1';
  const isLocalDbName = /local|test|demo|migration.?validation/i.test(dbName);
  const isGithubActionsSharedDevOverride =
    env.GITHUB_ACTIONS === 'true' && env.ALLOW_SHARED_DEV_DEMO_SEED === 'true';
  const isGithubActionsSharedDevSchema = schema === 'mejengueros_dev';

  if (!isLocalHost && !isLocalDbName && !isGithubActionsSharedDevOverride) {
    throw new Error(
      `ERROR: DATABASE_URL points to "${host}/${dbName}" which does not look like a local or demo database.\n` +
        `  Allowed: host is localhost/127.0.0.1, OR database name matches local|test|demo|migration_validation.\n` +
        `  Manual GitHub Actions dev seed override requires GITHUB_ACTIONS=true and ALLOW_SHARED_DEV_DEMO_SEED=true.\n` +
        `  To run the seed locally, use the disposable Docker database documented in app-backend/api/docker/.`
    );
  }

  if (isGithubActionsSharedDevOverride) {
    if (!isGithubActionsSharedDevSchema) {
      throw new Error(
        'ERROR: Manual GitHub Actions dev seed override requires DATABASE_URL to use schema=mejengueros_dev.'
      );
    }

    // Manual GitHub Actions dev seed override is constrained to the shared dev schema
    // `mejengueros_dev`; this workflow resets demo-owned data and all data attached to
    // demo courts, without wiping the whole database.
    console.log('GitHub Actions dev demo seed override enabled for shared dev database.');
  }
}

export function assertSeedEnvironment(env: SeedEnvironment = process.env): void {
  const allowDemoSeed = env.ALLOW_DEMO_SEED === 'true';
  const nodeEnv = env.NODE_ENV ?? 'development';

  if (!allowDemoSeed) {
    throw new Error('ERROR: Set ALLOW_DEMO_SEED=true to run this seed.');
  }

  if (nodeEnv === 'production') {
    throw new Error('ERROR: Seed cannot run against a production environment.');
  }

  assertLocalDatabase(env.DATABASE_URL ?? '', env);
}

// All demo identities use provider='demo' and demo emails remain exact so teardown can
// recover legacy partial rows safely without touching unrelated records.
const DEMO_PROVIDER = 'demo';
const DEMO_OWNER_SUBJECT = 'demo-owner-sub-00000001';
const DEMO_PLAYER_1_SUBJECT = 'demo-player-sub-00000001';
const DEMO_PLAYER_2_SUBJECT = 'demo-player-sub-00000002';
const DEMO_OWNER_EMAIL = 'demo-owner@mejengueros.demo';
const DEMO_PLAYER_1_EMAIL = 'demo-player1@mejengueros.demo';
const DEMO_PLAYER_2_EMAIL = 'demo-player2@mejengueros.demo';
const DEMO_EMAILS = [DEMO_OWNER_EMAIL, DEMO_PLAYER_1_EMAIL, DEMO_PLAYER_2_EMAIL];

const DEMO_PROVINCE_CODE = 'SJ';
const DEMO_CANTON_CODE = 'SJ-01';
const DEMO_PROVINCE_NAME = 'San Jose';
const DEMO_CANTON_NAME = 'San Jose';

const SERVICE_CATALOG_SEEDS: readonly ServiceCatalogSeed[] = [
  { name: 'Parqueo', scope: 'COMPLEX' },
  { name: 'Iluminacion', scope: 'COURT' },
  { name: 'Sintetico', scope: 'COURT' },
  { name: 'Natural', scope: 'COURT' },
  { name: 'Hibrido', scope: 'COURT' }
];

function sanitizeConnectionString(raw: string): string {
  const url = new URL(raw);
  url.searchParams.delete('connection_limit');
  url.searchParams.delete('pool_timeout');
  url.searchParams.delete('connect_timeout');
  return url.toString();
}

function timeAt(hour: number): string {
  return `1970-01-01T${String(hour).padStart(2, '0')}:00:00.000Z`;
}

function nextWeekdayAt(targetDay: number, hourUtc: number): Date {
  const now = new Date();
  const currentDay = now.getUTCDay();
  const daysUntil = ((targetDay - currentDay + 7) % 7) || 7;
  return new Date(
    Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() + daysUntil, hourUtc, 0, 0, 0)
  );
}

function daysAgoAt(days: number, hourUtc: number): Date {
  const now = new Date();
  return new Date(
    Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() - days, hourUtc, 0, 0, 0)
  );
}

export async function teardown(prisma: PrismaClient): Promise<void> {
  const demoIdentities = await prisma.userIdentity.findMany({
    where: { provider: DEMO_PROVIDER },
    select: { userId: true }
  });

  const demoUsersByEmail = await prisma.user.findMany({
    where: { email: { in: DEMO_EMAILS } },
    select: { id: true }
  });

  const demoUserIds = [
    ...new Set([...demoIdentities.map((identity) => identity.userId), ...demoUsersByEmail.map((user) => user.id)])
  ];

  if (demoUserIds.length === 0) return;

  const demoComplexes = await prisma.complex.findMany({
    where: { ownerId: { in: demoUserIds } },
    select: { id: true }
  });
  const demoComplexIds = demoComplexes.map((complex) => complex.id);

  const demoCourts = await prisma.court.findMany({
    where: { complexId: { in: demoComplexIds } },
    select: { id: true }
  });
  const demoCourtIds = demoCourts.map((court) => court.id);

  const demoReservations = await prisma.reservation.findMany({
    where: {
      OR: [{ courtId: { in: demoCourtIds } }, { userId: { in: demoUserIds } }]
    },
    select: { id: true }
  });
  const demoReservationIds = demoReservations.map((reservation) => reservation.id);

  await prisma.review.deleteMany({
    where: { reservationId: { in: demoReservationIds } }
  });
  await prisma.notification.deleteMany({
    where: {
      OR: [{ reservationId: { in: demoReservationIds } }, { userId: { in: demoUserIds } }]
    }
  });
  await prisma.reservation.deleteMany({ where: { id: { in: demoReservationIds } } });
  // CourtAvailabilityDay, CourtAvailability, and CourtService cascade from Court.
  await prisma.court.deleteMany({ where: { id: { in: demoCourtIds } } });
  // ComplexService cascades from Complex.
  await prisma.complex.deleteMany({ where: { id: { in: demoComplexIds } } });
  // UserIdentity and UserRole cascade from User.
  await prisma.user.deleteMany({ where: { id: { in: demoUserIds } } });
}

export async function ensureProvinceCatalog(prisma: PrismaClient): Promise<ProvinceSeedRecord> {
  const [provinceByCode, provinceByName] = await Promise.all([
    prisma.province.findUnique({
      where: { code: DEMO_PROVINCE_CODE },
      select: { id: true, code: true, name: true }
    }),
    prisma.province.findUnique({
      where: { name: DEMO_PROVINCE_NAME },
      select: { id: true, code: true, name: true }
    })
  ]);

  if (provinceByCode && provinceByName && provinceByCode.id !== provinceByName.id) {
    throw new Error(
      `Province seed conflict: code ${DEMO_PROVINCE_CODE} and name ${DEMO_PROVINCE_NAME} belong to different rows (${provinceByCode.id} vs ${provinceByName.id}).`
    );
  }

  const existingProvince = provinceByCode ?? provinceByName;

  if (!existingProvince) {
    return prisma.province.create({
      data: { code: DEMO_PROVINCE_CODE, name: DEMO_PROVINCE_NAME },
      select: { id: true, code: true, name: true }
    });
  }

  if (
    existingProvince.code !== DEMO_PROVINCE_CODE ||
    existingProvince.name !== DEMO_PROVINCE_NAME
  ) {
    return prisma.province.update({
      where: { id: existingProvince.id },
      data: { code: DEMO_PROVINCE_CODE, name: DEMO_PROVINCE_NAME },
      select: { id: true, code: true, name: true }
    });
  }

  return existingProvince;
}

export async function ensureCantonCatalog(
  prisma: PrismaClient,
  provinceId: string
): Promise<CantonSeedRecord> {
  const [cantonByCode, cantonByProvinceAndName] = await Promise.all([
    prisma.canton.findUnique({
      where: { code: DEMO_CANTON_CODE },
      select: { id: true, provinceId: true, code: true, name: true }
    }),
    prisma.canton.findFirst({
      where: { provinceId, name: DEMO_CANTON_NAME },
      select: { id: true, provinceId: true, code: true, name: true }
    })
  ]);

  if (cantonByCode && cantonByCode.provinceId !== provinceId) {
    throw new Error(
      `Canton seed conflict: code ${DEMO_CANTON_CODE} belongs to province ${cantonByCode.provinceId}, expected ${provinceId}.`
    );
  }

  if (
    cantonByCode &&
    cantonByProvinceAndName &&
    cantonByCode.id !== cantonByProvinceAndName.id
  ) {
    throw new Error(
      `Canton seed conflict: code ${DEMO_CANTON_CODE} and (${provinceId}, ${DEMO_CANTON_NAME}) belong to different rows (${cantonByCode.id} vs ${cantonByProvinceAndName.id}).`
    );
  }

  const existingCanton = cantonByCode ?? cantonByProvinceAndName;

  if (!existingCanton) {
    return prisma.canton.create({
      data: { code: DEMO_CANTON_CODE, name: DEMO_CANTON_NAME, provinceId },
      select: { id: true, provinceId: true, code: true, name: true }
    });
  }

  if (
    existingCanton.provinceId !== provinceId ||
    existingCanton.code !== DEMO_CANTON_CODE ||
    existingCanton.name !== DEMO_CANTON_NAME
  ) {
    return prisma.canton.update({
      where: { id: existingCanton.id },
      data: { provinceId, code: DEMO_CANTON_CODE, name: DEMO_CANTON_NAME },
      select: { id: true, provinceId: true, code: true, name: true }
    });
  }

  return existingCanton;
}

export async function seedSharedCatalogs(prisma: PrismaClient) {
  return Promise.all(
    SERVICE_CATALOG_SEEDS.map((service) =>
      prisma.serviceCatalog.upsert({
        where: { name: service.name },
        create: { name: service.name, scope: service.scope, isActive: true },
        update: { scope: service.scope, isActive: true }
      })
    )
  );
}

export async function seed(prisma: PrismaClient): Promise<void> {
  // Province and Canton are geographic catalogs — reconciled, never deleted in teardown.
  const province = await ensureProvinceCatalog(prisma);
  const canton = await ensureCantonCatalog(prisma, province.id);

  // ServiceCatalog entries are shared — reconciled, never deleted in teardown.
  const [svcParqueo, svcIluminacion, svcSintetico, svcNatural, svcHibrido] =
    await seedSharedCatalogs(prisma);

  const owner = await prisma.user.create({
    data: {
      email: DEMO_OWNER_EMAIL,
      name: 'Carlos Demo (Dueno)',
      identities: { create: { provider: DEMO_PROVIDER, providerSubject: DEMO_OWNER_SUBJECT } },
      roles: { create: { role: 'OWNER' } }
    }
  });

  const [player1, player2] = await Promise.all([
    prisma.user.create({
      data: {
        email: DEMO_PLAYER_1_EMAIL,
        name: 'Martin Demo (Jugador)',
        identities: { create: { provider: DEMO_PROVIDER, providerSubject: DEMO_PLAYER_1_SUBJECT } },
        roles: { create: { role: 'PLAYER' } }
      }
    }),
    prisma.user.create({
      data: {
        email: DEMO_PLAYER_2_EMAIL,
        name: 'Ana Demo (Jugadora)',
        identities: { create: { provider: DEMO_PROVIDER, providerSubject: DEMO_PLAYER_2_SUBJECT } },
        roles: { create: { role: 'PLAYER' } }
      }
    })
  ]);

  const complex = await prisma.complex.create({
    data: {
      ownerId: owner.id,
      provinceId: province.id,
      cantonId: canton.id,
      name: 'Complejo Demo Los Nogales',
      address: 'Av. Central 1234, San Jose, Costa Rica',
      latitude: 9.9281,
      longitude: -84.0907,
      services: {
        create: [{ serviceCatalogId: svcParqueo.id }]
      }
    }
  });

  const court = await prisma.court.create({
    data: {
      complexId: complex.id,
      name: 'Cancha 1 — Demo',
      services: {
        create: [
          { serviceCatalogId: svcIluminacion.id },
          { serviceCatalogId: svcSintetico.id },
          { serviceCatalogId: svcNatural.id },
          { serviceCatalogId: svcHibrido.id }
        ]
      },
      availability: {
        create: {
          startTime: timeAt(8),
          endTime: timeAt(22),
          days: {
            create: [
              { day: 'MONDAY' },
              { day: 'TUESDAY' },
              { day: 'WEDNESDAY' },
              { day: 'THURSDAY' },
              { day: 'FRIDAY' },
              { day: 'SATURDAY' }
            ]
          }
        }
      }
    }
  });

  // Future CONFIRMED slot — used to demonstrate the double-booking error.
  const takenStart = nextWeekdayAt(6, 10);
  const takenEnd = new Date(takenStart.getTime() + 60 * 60 * 1000);

  await prisma.reservation.create({
    data: {
      userId: player1.id,
      courtId: court.id,
      startsAt: takenStart,
      endsAt: takenEnd,
      status: 'CONFIRMED'
    }
  });

  // Past COMPLETED reservation — demonstrates post-game flow.
  const pastStart = daysAgoAt(7, 10);
  const pastEnd = new Date(pastStart.getTime() + 60 * 60 * 1000);

  const pastReservation = await prisma.reservation.create({
    data: {
      userId: player2.id,
      courtId: court.id,
      startsAt: pastStart,
      endsAt: pastEnd,
      status: 'COMPLETED',
      completedAt: pastEnd
    }
  });

  await prisma.review.create({
    data: {
      reservationId: pastReservation.id,
      rating: 5,
      comment: 'Excelente cancha, muy bien mantenida.'
    }
  });
}

async function main(): Promise<void> {
  assertSeedEnvironment(process.env);

  const databaseUrl = process.env.DATABASE_URL;
  if (!databaseUrl) throw new Error('DATABASE_URL is required');

  const pool = new Pool({ connectionString: sanitizeConnectionString(databaseUrl), max: 5 });
  const prisma = new PrismaClient({ adapter: new PrismaPg(pool) });

  try {
    console.log('Tearing down existing demo seed data...');
    await teardown(prisma);
    console.log('Inserting demo seed data...');
    await seed(prisma);
    console.log('Seed complete.');
    console.log(`  Owner:    ${DEMO_OWNER_SUBJECT}`);
    console.log(`  Player 1: ${DEMO_PLAYER_1_SUBJECT}  (holds next Saturday 10:00 UTC)`);
    console.log(`  Player 2: ${DEMO_PLAYER_2_SUBJECT}  (completed reservation last week)`);
  } finally {
    await prisma.$disconnect();
    await pool.end();
  }
}

if (require.main === module) {
  main().catch((err: unknown) => {
    console.error(err instanceof Error ? err.message : err);
    process.exit(1);
  });
}
