import { randomUUID } from 'node:crypto';
import { Prisma } from '@/generated/prisma/client';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';
import { createReservationCompletionWorkerApplicationContext } from '@/bootstrap/application-context';
import { CreateReviewPromptNotificationsUseCase } from '@/modules/notifications/application/use-cases/create-review-prompt-notifications.use-case';

const DEFAULT_EMAIL = 'barahona0498@gmail.com';
const DEFAULT_COUNT = 4;
const DEFAULT_COURT_NAME = 'Cancha de prueba';

async function main(): Promise<void> {
  const email = readArg('--email') ?? DEFAULT_EMAIL;
  const count = Number(readArg('--count') ?? DEFAULT_COUNT);
  const courtName = readArg('--court-name') ?? DEFAULT_COURT_NAME;
  const reset = readBooleanArg('--reset');

  if (!Number.isInteger(count) || count < 1 || count > 20) {
    throw new Error('--count must be an integer from 1 to 20.');
  }

  const app = await createReservationCompletionWorkerApplicationContext();

  try {
    const prisma = app.get(PrismaService);
    const createReviewPromptNotifications = app.get(CreateReviewPromptNotificationsUseCase);
    const user = await prisma.user.findUnique({ where: { email } });

    if (user == null) {
      throw new Error(`User not found for email ${email}.`);
    }

    const court = await prisma.court.findFirst({
      where: {
        name: courtName,
        deletedAt: null,
        complex: { deletedAt: null }
      },
      select: { id: true, name: true }
    });

    if (court == null) {
      throw new Error(`Court not found with name ${courtName}.`);
    }

    if (reset) {
      await deleteUserNotificationFixtures(prisma, user.id);
    }
    const reservationIds = await createExpiredReservations(prisma, {
      count,
      courtId: court.id,
      userId: user.id
    });

    const completedReservations = await completeReservationsById(prisma, reservationIds);
    const createdNotifications =
      await createReviewPromptNotifications.execute(completedReservations);
    const notificationCount = await prisma.notification.count({
      where: {
        userId: user.id,
        status: 'PENDING'
      }
    });

    console.log(
      JSON.stringify(
        {
          email,
          courtName: court.name,
          reset,
          completedReservations: completedReservations.length,
          createdNotifications,
          pendingNotifications: notificationCount
        },
        null,
        2
      )
    );
  } finally {
    await app.close();
  }
}

async function deleteUserNotificationFixtures(
  prisma: PrismaService,
  userId: string
): Promise<void> {
  await prisma.$transaction([
    prisma.$executeRaw(
      Prisma.sql`
        DELETE FROM "mejengueros_dev"."Review" review
        USING "mejengueros_dev"."Reservation" reservation
        WHERE review."reservationId" = reservation.id
          AND reservation."userId" = ${userId}
      `
    ),
    prisma.notification.deleteMany({ where: { userId } }),
    prisma.reservation.deleteMany({ where: { userId } })
  ]);
}

async function createExpiredReservations(
  prisma: PrismaService,
  input: {
    count: number;
    courtId: string;
    userId: string;
  }
): Promise<string[]> {
  const baseStart = Date.now() - 48 * 60 * 60 * 1000;
  const reservationIds: string[] = [];

  for (let index = 0; index < input.count; index++) {
    const reservationId = randomUUID();
    const startsAt = new Date(baseStart - index * 2 * 60 * 60 * 1000);
    const endsAt = new Date(startsAt.getTime() + 60 * 60 * 1000);

    await prisma.reservation.create({
      data: {
        id: reservationId,
        userId: input.userId,
        courtId: input.courtId,
        startsAt,
        endsAt,
        status: 'CONFIRMED'
      }
    });
    reservationIds.push(reservationId);
  }

  return reservationIds;
}

async function completeReservationsById(
  prisma: PrismaService,
  reservationIds: string[]
): Promise<Array<{ id: string; userId: string }>> {
  const rows = await prisma.$queryRaw<Array<{ id: string; userId: string }>>(
    Prisma.sql`
      UPDATE "mejengueros_dev"."Reservation"
      SET
        "status" = CAST('COMPLETED' AS "mejengueros_dev"."ReservationStatus"),
        "completedAt" = "endsAt",
        "updatedAt" = CURRENT_TIMESTAMP
      WHERE id IN (${Prisma.join(reservationIds)})
      RETURNING "id", "userId"
    `
  );

  return rows;
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
