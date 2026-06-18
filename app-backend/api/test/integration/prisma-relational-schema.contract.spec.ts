import {
  extractPrismaBlock,
  loadPrismaRelationalSchemaContract,
  prismaFieldPattern,
  sqlFragmentPattern
} from '@/shared/infrastructure/database/prisma-relational-schema.contract';

describe('Prisma relational MVP schema contract', () => {
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
});
