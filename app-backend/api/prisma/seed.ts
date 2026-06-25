import 'dotenv/config';
import { PrismaPg } from '@prisma/adapter-pg';
import { Pool } from 'pg';
import { PrismaClient } from '../src/generated/prisma/client';

// Guard: only run against known local/demo databases.
const allowDemoSeed = process.env.ALLOW_DEMO_SEED === 'true';
const nodeEnv = process.env.NODE_ENV ?? 'development';

if (!allowDemoSeed) {
  console.error('ERROR: Set ALLOW_DEMO_SEED=true to run this seed.');
  process.exit(1);
}

if (nodeEnv === 'production') {
  console.error('ERROR: Seed cannot run against a production environment.');
  process.exit(1);
}

function assertLocalDatabase(rawUrl: string): void {
  let parsed: URL;
  try {
    parsed = new URL(rawUrl);
  } catch {
    console.error('ERROR: DATABASE_URL is not a valid URL.');
    process.exit(1);
  }

  const host = parsed.hostname;
  const dbName = parsed.pathname.replace(/^\//, '');
  const schema = parsed.searchParams.get('schema');

  const isLocalHost = host === 'localhost' || host === '127.0.0.1';
  const isLocalDbName = /local|test|demo|migration.?validation/i.test(dbName);
  const isGithubActionsSharedDevOverride =
    process.env.GITHUB_ACTIONS === 'true' && process.env.ALLOW_SHARED_DEV_DEMO_SEED === 'true';
  const isGithubActionsSharedDevSchema = schema === 'mejengueros_dev';

  if (!isLocalHost && !isLocalDbName && !isGithubActionsSharedDevOverride) {
    console.error(
      `ERROR: DATABASE_URL points to "${host}/${dbName}" which does not look like a local or demo database.\n` +
      `  Allowed: host is localhost/127.0.0.1, OR database name matches local|test|demo|migration_validation.\n` +
      `  Manual GitHub Actions dev seed override requires GITHUB_ACTIONS=true and ALLOW_SHARED_DEV_DEMO_SEED=true.\n` +
      `  To run the seed locally, use the disposable Docker database documented in app-backend/api/docker/.`
    );
    process.exit(1);
  }

  if (isGithubActionsSharedDevOverride) {
    if (!isGithubActionsSharedDevSchema) {
      console.error(
        'ERROR: Manual GitHub Actions dev seed override requires DATABASE_URL to use schema=mejengueros_dev.'
      );
      process.exit(1);
    }

    // Manual GitHub Actions dev seed override is constrained to the shared dev schema
    // `mejengueros_dev`; this workflow resets demo-owned data and all data attached to
    // demo courts, without wiping the whole database.
    console.log('GitHub Actions dev demo seed override enabled for shared dev database.');
  }
}

assertLocalDatabase(process.env.DATABASE_URL ?? '');

// All demo identities use provider='demo' so teardown can find them by provider.
const DEMO_PROVIDER = 'demo';
const DEMO_OWNER_SUBJECT = 'demo-owner-sub-00000001';
const DEMO_PLAYER_1_SUBJECT = 'demo-player-sub-00000001';
const DEMO_PLAYER_2_SUBJECT = 'demo-player-sub-00000002';

const DEMO_PROVINCE_CODE = 'SJ';
const DEMO_CANTON_CODE = 'SJ-01';

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
  return new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() + daysUntil, hourUtc, 0, 0, 0));
}

function daysAgoAt(days: number, hourUtc: number): Date {
  const now = new Date();
  return new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() - days, hourUtc, 0, 0, 0));
}

async function teardown(prisma: PrismaClient): Promise<void> {
  const demoIdentities = await prisma.userIdentity.findMany({
    where: { provider: DEMO_PROVIDER },
    select: { userId: true }
  });

  if (demoIdentities.length === 0) return;

  const demoUserIds = [...new Set(demoIdentities.map((i) => i.userId))];

  const demoComplexes = await prisma.complex.findMany({
    where: { ownerId: { in: demoUserIds } },
    select: { id: true }
  });
  const demoComplexIds = demoComplexes.map((c) => c.id);

  const demoCourts = await prisma.court.findMany({
    where: { complexId: { in: demoComplexIds } },
    select: { id: true }
  });
  const demoCourtIds = demoCourts.map((c) => c.id);

  // Delete all data associated with demo courts (not just demo users) to avoid FK violations
  // when non-demo users have reserved demo courts.
  await prisma.review.deleteMany({ where: { reservation: { courtId: { in: demoCourtIds } } } });
  await prisma.notification.deleteMany({ where: { reservation: { courtId: { in: demoCourtIds } } } });
  await prisma.reservation.deleteMany({ where: { courtId: { in: demoCourtIds } } });
  // CourtAvailabilityDay, CourtAvailability, and CourtService cascade from Court.
  await prisma.court.deleteMany({ where: { id: { in: demoCourtIds } } });
  // ComplexService cascades from Complex.
  await prisma.complex.deleteMany({ where: { id: { in: demoComplexIds } } });
  // UserIdentity and UserRole cascade from User.
  await prisma.user.deleteMany({ where: { id: { in: demoUserIds } } });
}

async function seed(prisma: PrismaClient): Promise<void> {
  // Province and Canton are geographic catalogs — upsert, never deleted in teardown.
  const province = await prisma.province.upsert({
    where: { code: DEMO_PROVINCE_CODE },
    create: { code: DEMO_PROVINCE_CODE, name: 'San Jose' },
    update: {}
  });

  const canton = await prisma.canton.upsert({
    where: { code: DEMO_CANTON_CODE },
    create: { code: DEMO_CANTON_CODE, name: 'San Jose', provinceId: province.id },
    update: {}
  });

  // ServiceCatalog entries are shared — upsert, never deleted in teardown.
  const [svcParqueo, svcIluminacion, svcSintetico, svcNatural, svcHibrido] = await Promise.all([
    prisma.serviceCatalog.upsert({
      where: { name: 'Parqueo' },
      create: { name: 'Parqueo', scope: 'COMPLEX' },
      update: {}
    }),
    prisma.serviceCatalog.upsert({
      where: { name: 'Iluminacion' },
      create: { name: 'Iluminacion', scope: 'COURT' },
      update: {}
    }),
    prisma.serviceCatalog.upsert({
      where: { name: 'Sintetico' },
      create: { name: 'Sintetico', scope: 'COURT' },
      update: {}
    }),
    prisma.serviceCatalog.upsert({
      where: { name: 'Natural' },
      create: { name: 'Natural', scope: 'COURT' },
      update: {}
    }),
    prisma.serviceCatalog.upsert({
      where: { name: 'Hibrido' },
      create: { name: 'Hibrido', scope: 'COURT' },
      update: {}
    })
  ]);

  const owner = await prisma.user.create({
    data: {
      email: 'demo-owner@mejengueros.demo',
      name: 'Carlos Demo (Dueno)',
      identities: { create: { provider: DEMO_PROVIDER, providerSubject: DEMO_OWNER_SUBJECT } },
      roles: { create: { role: 'OWNER' } }
    }
  });

  const [player1, player2] = await Promise.all([
    prisma.user.create({
      data: {
        email: 'demo-player1@mejengueros.demo',
        name: 'Martin Demo (Jugador)',
        identities: { create: { provider: DEMO_PROVIDER, providerSubject: DEMO_PLAYER_1_SUBJECT } },
        roles: { create: { role: 'PLAYER' } }
      }
    }),
    prisma.user.create({
      data: {
        email: 'demo-player2@mejengueros.demo',
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
  const takenStart = nextWeekdayAt(6, 10); // next Saturday at 10:00 UTC
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

main().catch((err: unknown) => {
  console.error(err);
  process.exit(1);
});
