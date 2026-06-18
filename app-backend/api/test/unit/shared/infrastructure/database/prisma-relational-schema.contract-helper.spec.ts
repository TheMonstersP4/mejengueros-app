import {
  normalizeWhitespace,
  prismaFieldPattern,
  sqlFragmentPattern
} from '@/shared/infrastructure/database/prisma-relational-schema.contract';

describe('prisma relational schema contract helpers', () => {
  it('matches Prisma fields without depending on exact column spacing', () => {
    const block = [
      'model UserRole {',
      '  userId        String',
      '  role   UserRoleKind',
      '  days    CourtAvailabilityDay[]',
      '}'
    ].join('\n');

    expect(block).toMatch(prismaFieldPattern('userId', 'String'));
    expect(block).toMatch(prismaFieldPattern('role', 'UserRoleKind'));
    expect(block).toMatch(prismaFieldPattern('days', 'CourtAvailabilityDay[]'));
  });

  it('matches SQL fragments across formatting differences', () => {
    const migration = [
      'CREATE UNIQUE INDEX "Reservation_confirmed_court_slot_key"',
      'ON "mejengueros_dev"."Reservation" ("courtId", "startsAt")',
      'WHERE "status" = \'CONFIRMED\';'
    ].join('\n');

    expect(migration).toMatch(
      sqlFragmentPattern(
        'CREATE UNIQUE INDEX "Reservation_confirmed_court_slot_key" ON "mejengueros_dev"."Reservation" ("courtId", "startsAt") WHERE "status" = \'CONFIRMED\';'
      )
    );
    expect(normalizeWhitespace(migration)).toBe(
      'CREATE UNIQUE INDEX "Reservation_confirmed_court_slot_key" ON "mejengueros_dev"."Reservation" ("courtId", "startsAt") WHERE "status" = \'CONFIRMED\';'
    );
  });
});
