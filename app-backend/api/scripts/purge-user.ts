import { PrismaService } from '@/shared/infrastructure/database/prisma.service';
import { createReservationCompletionWorkerApplicationContext } from '@/bootstrap/application-context';

/**
 * Hard-deletes a user and every entity that hangs off them.
 *
 * Most relations to User are `onDelete: Restrict` (Reservation.user, Notification.user,
 * Complex.owner), so a plain user delete fails. The order below is the only one that
 * satisfies every foreign key.
 *
 * Blast radius: when the user owns complexes, their courts are referenced by OTHER users'
 * reservations, reviews and notifications. Those rows are Restrict too, so purging the owner
 * necessarily destroys that data. Pass --include-owned-complexes=true to accept it; without
 * the flag the script refuses to touch an owner and explains what it found.
 *
 * Dry run by default — nothing is committed unless --execute=true is passed.
 */

type PurgeCounts = Record<string, number>;

class DryRunRollback extends Error {
  constructor(readonly counts: PurgeCounts) {
    super('dry-run');
  }
}

async function main(): Promise<void> {
  const email = readArg('--email');
  const execute = readBooleanArg('--execute');
  const includeOwnedComplexes = readBooleanArg('--include-owned-complexes');

  if (email == null || email.length === 0) {
    throw new Error('--email=<address> is required.');
  }

  const app = await createReservationCompletionWorkerApplicationContext();

  try {
    const prisma = app.get(PrismaService);
    const user = await prisma.user.findFirst({
      where: { email: { equals: email, mode: 'insensitive' } },
      select: { id: true, email: true }
    });

    if (user == null) {
      throw new Error(`User not found for email ${email}.`);
    }

    const scope = await resolveScope(prisma, user, includeOwnedComplexes);
    const counts = await runPurge(prisma, user.id, scope, execute);

    console.log(
      JSON.stringify(
        {
          email: user.email,
          userId: user.id,
          mode: execute ? 'EXECUTED' : 'DRY_RUN',
          includeOwnedComplexes,
          scope: {
            complexes: scope.complexIds.length,
            courts: scope.courtIds.length,
            reservations: scope.reservationIds.length,
            reviews: scope.reviewIds.length,
            imageUploads: scope.imageUploadIds.length,
            collateralReservations: scope.collateralReservationCount
          },
          deleted: counts
        },
        null,
        2
      )
    );

    if (!execute) {
      console.log('\nDry run — rolled back. Re-run with --execute=true to commit.');
    }
  } finally {
    await app.close();
  }
}

interface PurgeScope {
  complexIds: string[];
  courtIds: string[];
  reservationIds: string[];
  reviewIds: string[];
  imageUploadIds: string[];
  collateralReservationCount: number;
}

async function resolveScope(
  prisma: PrismaService,
  user: { id: string; email: string },
  includeOwnedComplexes: boolean
): Promise<PurgeScope> {
  const userId = user.id;
  const ownedComplexes = await prisma.complex.findMany({
    where: { ownerId: userId },
    select: { id: true, name: true }
  });

  if (ownedComplexes.length > 0 && !includeOwnedComplexes) {
    const names = ownedComplexes.map((complex) => complex.name).join(', ');

    throw new Error(
      `User owns ${ownedComplexes.length} complex(es) [${names}]. ` +
        'Complex.owner is onDelete: Restrict, so the user cannot be deleted while they remain ' +
        'the owner. Deleting those complexes also deletes other users\' reservations and reviews ' +
        'on their courts. Re-run with --include-owned-complexes=true to accept that, or reassign ' +
        'ownership first.'
    );
  }

  const complexIds = ownedComplexes.map((complex) => complex.id);
  const courts = await prisma.court.findMany({
    where: { complexId: { in: complexIds } },
    select: { id: true }
  });
  const courtIds = courts.map((court) => court.id);

  // Reservations made BY the user, plus every reservation made by ANYONE on the user's courts.
  const reservations = await prisma.reservation.findMany({
    where: { OR: [{ userId }, { courtId: { in: courtIds } }] },
    select: { id: true, userId: true }
  });
  const reservationIds = reservations.map((reservation) => reservation.id);
  const collateralReservationCount = reservations.filter(
    (reservation) => reservation.userId !== userId
  ).length;

  const reviews = await prisma.review.findMany({
    where: { reservationId: { in: reservationIds } },
    select: { id: true }
  });
  const identities = await prisma.userIdentity.findMany({
    where: { userId },
    select: { providerSubject: true }
  });
  const imageUploads = await prisma.imageUpload.findMany({
    where: {
      OR: [
        { ownerEmail: { equals: user.email, mode: 'insensitive' } },
        { ownerSub: { in: identities.map((identity) => identity.providerSubject) } }
      ]
    },
    select: { id: true }
  });

  return {
    complexIds,
    courtIds,
    reservationIds,
    reviewIds: reviews.map((review) => review.id),
    imageUploadIds: imageUploads.map((imageUpload) => imageUpload.id),
    collateralReservationCount
  };
}

async function runPurge(
  prisma: PrismaService,
  userId: string,
  scope: PurgeScope,
  execute: boolean
): Promise<PurgeCounts> {
  const purge = async (tx: PrismaService): Promise<PurgeCounts> => {
    const { complexIds, courtIds, reservationIds, reviewIds, imageUploadIds } = scope;

    return {
      // Cascades from Review, but deleted explicitly so the report is accurate.
      ReviewQuestionnaireAnswer: (
        await tx.reviewQuestionnaireAnswer.deleteMany({ where: { reviewId: { in: reviewIds } } })
      ).count,
      Review: (await tx.review.deleteMany({ where: { id: { in: reviewIds } } })).count,
      // By reservation OR user: a reservation on the user's court can carry another user's notification.
      Notification: (
        await tx.notification.deleteMany({
          where: { OR: [{ reservationId: { in: reservationIds } }, { userId }] }
        })
      ).count,
      Reservation: (await tx.reservation.deleteMany({ where: { id: { in: reservationIds } } })).count,
      CourtAvailabilityDay: (
        await tx.courtAvailabilityDay.deleteMany({
          where: { availability: { courtId: { in: courtIds } } }
        })
      ).count,
      CourtAvailability: (
        await tx.courtAvailability.deleteMany({ where: { courtId: { in: courtIds } } })
      ).count,
      CourtService: (await tx.courtService.deleteMany({ where: { courtId: { in: courtIds } } })).count,
      Court: (await tx.court.deleteMany({ where: { id: { in: courtIds } } })).count,
      ComplexService: (
        await tx.complexService.deleteMany({ where: { complexId: { in: complexIds } } })
      ).count,
      Complex: (await tx.complex.deleteMany({ where: { id: { in: complexIds } } })).count,
      UserRole: (await tx.userRole.deleteMany({ where: { userId } })).count,
      UserIdentity: (await tx.userIdentity.deleteMany({ where: { userId } })).count,
      User: (await tx.user.deleteMany({ where: { id: userId } })).count,
      // Last: Court.imageUpload and Review.evidenceImageUpload are SetNull, so the
      // referencing rows must be gone first.
      ImageUpload: (await tx.imageUpload.deleteMany({ where: { id: { in: imageUploadIds } } })).count
    };
  };

  if (execute) {
    return prisma.$transaction((tx) => purge(tx as unknown as PrismaService));
  }

  try {
    await prisma.$transaction(async (tx) => {
      throw new DryRunRollback(await purge(tx as unknown as PrismaService));
    });
  } catch (error: unknown) {
    if (error instanceof DryRunRollback) {
      return error.counts;
    }

    throw error;
  }

  throw new Error('Dry run did not roll back as expected.');
}

function readArg(name: string): string | undefined {
  const argument = process.argv.find((value) => value.startsWith(`${name}=`));

  return argument?.slice(name.length + 1);
}

function readBooleanArg(name: string): boolean {
  return readArg(name)?.toLowerCase() === 'true';
}

main().catch((error: unknown) => {
  const message = error instanceof Error ? error.message : 'Unexpected script failure.';

  console.error(message);
  process.exitCode = 1;
});
