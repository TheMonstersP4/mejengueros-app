import { randomUUID } from 'node:crypto';

import { Client } from 'pg';

import {
  extractPrismaBlock,
  loadPrismaRelationalSchemaContract,
  prismaFieldPattern,
  sqlFragmentPattern
} from '@/shared/infrastructure/database/prisma-relational-schema.contract';

const DEFAULT_TEST_DATABASE_URL = 'postgresql://user:password@localhost:5432/appdb';
const liveDatabaseUrl =
  process.env.PRISMA_MIGRATION_CONTRACT_DATABASE_URL ?? process.env.DATABASE_URL;
const runLiveDatabaseContract =
  typeof liveDatabaseUrl === 'string' &&
  liveDatabaseUrl.length > 0 &&
  liveDatabaseUrl !== DEFAULT_TEST_DATABASE_URL;

describe('Prisma relational MVP schema contract', () => {
  it('adds Province and Canton catalogs for controlled Costa Rica location data', () => {
    const contract = loadPrismaRelationalSchemaContract();
    const provinceModel = extractPrismaBlock(contract.schema, 'model', 'Province');
    const cantonModel = extractPrismaBlock(contract.schema, 'model', 'Canton');

    expect(provinceModel).toMatch(prismaFieldPattern('code', 'String'));
    expect(provinceModel).toMatch(prismaFieldPattern('name', 'String'));
    expect(provinceModel).toMatch(prismaFieldPattern('cantons', 'Canton[]'));
    expect(cantonModel).toMatch(prismaFieldPattern('provinceId', 'String'));
    expect(cantonModel).toMatch(prismaFieldPattern('code', 'String'));
    expect(cantonModel).toContain('@@unique([id, provinceId])');
    expect(cantonModel).toContain('@@unique([provinceId, name])');
    expect(contract.migration).toMatch(
      sqlFragmentPattern(
        'CREATE TABLE "mejengueros_dev"."Province" ('
      )
    );
    expect(contract.migration).toMatch(
      sqlFragmentPattern(
        'CREATE TABLE "mejengueros_dev"."Canton" ('
      )
    );
  });

  it('keeps complex address while adding optional province, canton, and map coordinates', () => {
    const contract = loadPrismaRelationalSchemaContract();
    const complexModel = extractPrismaBlock(contract.schema, 'model', 'Complex');

    expect(complexModel).toMatch(prismaFieldPattern('provinceId', 'String?'));
    expect(complexModel).toMatch(prismaFieldPattern('cantonId', 'String?'));
    expect(complexModel).toMatch(prismaFieldPattern('address', 'String'));
    expect(complexModel).toMatch(prismaFieldPattern('latitude', 'Float?'));
    expect(complexModel).toMatch(prismaFieldPattern('longitude', 'Float?'));
    expect(complexModel).toMatch(
      /canton\s+Canton\?\s+@relation\(fields:\s*\[cantonId,\s*provinceId\],\s*references:\s*\[id,\s*provinceId\],\s*onDelete:\s*Restrict\)/
    );
    expect(contract.migration).toMatch(
      sqlFragmentPattern(
        'ALTER TABLE "mejengueros_dev"."Complex" ADD COLUMN "provinceId" TEXT, ADD COLUMN "cantonId" TEXT, ADD COLUMN "latitude" DOUBLE PRECISION, ADD COLUMN "longitude" DOUBLE PRECISION;'
      )
    );
  });

  it('enforces that a complex canton belongs to the same persisted province', () => {
    const contract = loadPrismaRelationalSchemaContract();

    expect(contract.migration).toMatch(
      sqlFragmentPattern(
        'ALTER TABLE "mejengueros_dev"."Complex" DROP CONSTRAINT IF EXISTS "Complex_cantonId_fkey"'
      )
    );
    expect(contract.migration).toMatch(
      sqlFragmentPattern(
        'CREATE UNIQUE INDEX "Canton_id_provinceId_key" ON "mejengueros_dev"."Canton"("id", "provinceId")'
      )
    );
    expect(contract.migration).toMatch(
      sqlFragmentPattern(
        'ADD CONSTRAINT "Complex_canton_requires_province_check" CHECK ("cantonId" IS NULL OR "provinceId" IS NOT NULL)'
      )
    );
    expect(contract.migration).toMatch(
      sqlFragmentPattern(
        'ADD CONSTRAINT "Complex_canton_matches_province_fkey" FOREIGN KEY ("cantonId", "provinceId") REFERENCES "mejengueros_dev"."Canton"("id", "provinceId")'
      )
    );
  });

  it('supports non-exclusive multi-role users through a join model unique on user and role', () => {
    const contract = loadPrismaRelationalSchemaContract();
    const userRoleModel = extractPrismaBlock(contract.schema, 'model', 'UserRole');

    expect(userRoleModel).toMatch(prismaFieldPattern('userId', 'String'));
    expect(userRoleModel).toMatch(prismaFieldPattern('role', 'UserRoleKind'));
    expect(userRoleModel).toContain('@@unique([userId, role])');
    expect(contract.migration).toMatch(
      sqlFragmentPattern(
        'CREATE UNIQUE INDEX "UserRole_userId_role_key" ON "mejengueros_dev"."UserRole"("userId", "role")'
      )
    );
  });

  it('keeps ServiceCatalog closed and global for MVP while exposing lifecycle fields', () => {
    const contract = loadPrismaRelationalSchemaContract();
    const serviceCatalogModel = extractPrismaBlock(
      contract.schema,
      'model',
      'ServiceCatalog'
    );

    expect(serviceCatalogModel).toMatch(prismaFieldPattern('scope', 'ServiceScope'));
    expect(serviceCatalogModel).toMatch(prismaFieldPattern('isActive', 'Boolean'));
    expect(serviceCatalogModel).toMatch(prismaFieldPattern('createdAt', 'DateTime'));
    expect(serviceCatalogModel).toMatch(prismaFieldPattern('updatedAt', 'DateTime'));
    expect(serviceCatalogModel).not.toContain('ownerId');
    expect(serviceCatalogModel).not.toContain('isGlobal');
  });

  it('stores shared availability ranges separately from selected weekdays', () => {
    const contract = loadPrismaRelationalSchemaContract();
    const availabilityModel = extractPrismaBlock(
      contract.schema,
      'model',
      'CourtAvailability'
    );
    const availabilityDayModel = extractPrismaBlock(
      contract.schema,
      'model',
      'CourtAvailabilityDay'
    );

    expect(availabilityModel).toMatch(prismaFieldPattern('startTime', 'DateTime'));
    expect(availabilityModel).toMatch(prismaFieldPattern('endTime', 'DateTime'));
    expect(availabilityModel).toMatch(
      prismaFieldPattern('days', 'CourtAvailabilityDay[]')
    );
    expect(availabilityDayModel).toMatch(
      prismaFieldPattern('availabilityId', 'String')
    );
    expect(availabilityDayModel).toMatch(prismaFieldPattern('day', 'Weekday'));
    expect(availabilityDayModel).not.toContain('startTime');
    expect(availabilityDayModel).not.toContain('endTime');
  });

  it('limits reservation statuses and keeps reservations tied to users and courts only', () => {
    const contract = loadPrismaRelationalSchemaContract();
    const reservationStatusEnum = extractPrismaBlock(
      contract.schema,
      'enum',
      'ReservationStatus'
    );
    const reservationModel = extractPrismaBlock(
      contract.schema,
      'model',
      'Reservation'
    );

    expect(reservationStatusEnum).toContain('CONFIRMED');
    expect(reservationStatusEnum).toContain('CANCELLED');
    expect(reservationStatusEnum).toContain('COMPLETED');
    expect(reservationStatusEnum).not.toContain('PENDING');
    expect(reservationModel).toMatch(prismaFieldPattern('userId', 'String'));
    expect(reservationModel).toMatch(prismaFieldPattern('courtId', 'String'));
    expect(reservationModel).not.toContain('complexId');
  });

  it('uses a partial unique confirmed-slot index so cancelled reservations can be rebooked', () => {
    const contract = loadPrismaRelationalSchemaContract();

    expect(contract.migration).toContain(
      'CREATE UNIQUE INDEX "Reservation_confirmed_court_slot_key"'
    );
    expect(contract.migration).toMatch(
      sqlFragmentPattern(`WHERE "status" = 'CONFIRMED';`)
    );
    expect(contract.migration).not.toMatch(
      /CREATE UNIQUE INDEX\s+"[^"]+"\s+ON\s+"mejengueros_dev"\."Reservation"\s*\("courtId",\s*"startsAt"\)\s*;/
    );
  });

  it('represents review uniqueness and rating bounds in schema and migration contracts', () => {
    const contract = loadPrismaRelationalSchemaContract();
    const reviewModel = extractPrismaBlock(contract.schema, 'model', 'Review');

    expect(reviewModel).toMatch(prismaFieldPattern('reservationId', 'String'));
    expect(reviewModel).toContain('@unique');
    expect(reviewModel).toMatch(prismaFieldPattern('rating', 'Int'));
    expect(contract.migration).toMatch(
      sqlFragmentPattern(
        'CONSTRAINT "Review_rating_range_check" CHECK ("rating" BETWEEN 1 AND 5)'
      )
    );
    expect(contract.migration).toMatch(
      sqlFragmentPattern(
        'CREATE UNIQUE INDEX "Review_reservationId_key" ON "mejengueros_dev"."Review"("reservationId")'
      )
    );
  });

  it('keeps the one-hour reservation duration check in the migration', () => {
    const contract = loadPrismaRelationalSchemaContract();

    expect(contract.migration).toMatch(
      sqlFragmentPattern(
        'CONSTRAINT "Reservation_one_hour_duration_check" CHECK ("endsAt" = "startsAt" + INTERVAL \'1 hour\')'
      )
    );
  });

  (runLiveDatabaseContract ? describe : describe.skip)(
    'live migration invariant',
    () => {
      let client: Client;

      beforeAll(async () => {
        client = new Client({ connectionString: liveDatabaseUrl });
        await client.connect();
      });

      afterAll(async () => {
        await client.end();
      });

      beforeEach(async () => {
        await client.query('BEGIN');
      });

      afterEach(async () => {
        await client.query('ROLLBACK');
      });

      it('rejects a complex whose canton belongs to a different province', async () => {
        const ownerId = randomUUID();
        const selectedProvinceId = randomUUID();
        const differentProvinceId = randomUUID();
        const cantonId = randomUUID();
        const complexId = randomUUID();

        await client.query(
          `INSERT INTO "mejengueros_dev"."User" ("id", "cognitoSub", "email", "status", "createdAt", "updatedAt")
           VALUES ($1, $2, $3, 'ACTIVE', NOW(), NOW())`,
          [ownerId, `judgment-day-owner-${ownerId}`, `judgment-day-owner-${ownerId}@example.test`]
        );

        await client.query(
          `INSERT INTO "mejengueros_dev"."Province" ("id", "code", "name", "createdAt", "updatedAt")
           VALUES ($1, $2, $3, NOW(), NOW()), ($4, $5, $6, NOW(), NOW())`,
          [
            selectedProvinceId,
            `P-${selectedProvinceId.slice(0, 8)}`,
            `Province ${selectedProvinceId.slice(0, 8)}`,
            differentProvinceId,
            `P-${differentProvinceId.slice(0, 8)}`,
            `Province ${differentProvinceId.slice(0, 8)}`
          ]
        );

        await client.query(
          `INSERT INTO "mejengueros_dev"."Canton" ("id", "provinceId", "code", "name", "createdAt", "updatedAt")
           VALUES ($1, $2, $3, $4, NOW(), NOW())`,
          [
            cantonId,
            selectedProvinceId,
            `C-${cantonId.slice(0, 8)}`,
            `Canton ${cantonId.slice(0, 8)}`
          ]
        );

        await expect(
          client.query(
            `INSERT INTO "mejengueros_dev"."Complex" (
               "id", "ownerId", "provinceId", "cantonId", "name", "address", "status", "createdAt", "updatedAt"
             ) VALUES ($1, $2, $3, $4, $5, $6, 'ACTIVE', NOW(), NOW())`,
            [
              complexId,
              ownerId,
              differentProvinceId,
              cantonId,
              'Judgment Day Complex',
              'Invariant Avenue 161'
            ]
          )
        ).rejects.toMatchObject({
          code: '23503',
          constraint: 'Complex_canton_matches_province_fkey'
        });
      });
    }
  );
});
