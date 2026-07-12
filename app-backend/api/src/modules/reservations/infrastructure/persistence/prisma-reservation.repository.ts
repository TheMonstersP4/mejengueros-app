import { Inject, Injectable } from '@nestjs/common';
import { Prisma } from '@/generated/prisma/client';
import { costaRicaBusinessDayBounds } from '@/shared/domain/time/costa-rica-business-time';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import { OwnerReservationCourtNotAccessibleError } from '../../domain/errors/owner-reservation-court-not-accessible.error';
import { ReservationConflictError } from '../../domain/errors/reservation-conflict.error';
import { ReservationCourtNotFoundError } from '../../domain/errors/reservation-court-not-found.error';
import type {
  ICreateConfirmedReservationCommand,
  ICompleteExpiredReservationsCommand,
  ICompletedReservationSnapshot,
  IFindMyReservationsQuery,
  IListOwnerReservationsQuery,
  IMyReservationSnapshot,
  IMyReservationsSnapshotGroups,
  IOwnerReservationSnapshot,
  IOwnerReservationsSnapshotGroups,
  IReservationRepository,
  IReservationSnapshot,
  IReservationWindowQuery,
  IReservationWindowSnapshot
} from '../../domain/repositories/reservation.repository';

interface IReservationPersistenceClient {
  $queryRaw<T = unknown>(
    query: TemplateStringsArray | Prisma.Sql,
    ...values: unknown[]
  ): Promise<T>;
  $executeRaw: PrismaService['$executeRaw'];
  court: {
    findFirst: PrismaService['court']['findFirst'];
    findMany: PrismaService['court']['findMany'];
  };
  reservation: {
    create: PrismaService['reservation']['create'];
    findMany: PrismaService['reservation']['findMany'];
  };
}

const OWNER_RESERVATIONS_COGNITO_NATIVE_PROVIDER = 'Cognito';

@Injectable()
export class PrismaReservationRepository implements IReservationRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: IReservationPersistenceClient
  ) {}

  async getReservationWindow(
    query: IReservationWindowQuery
  ): Promise<IReservationWindowSnapshot> {
    const { start: dayStart, end: dayEnd } = costaRicaBusinessDayBounds(query.date);

    const court = await this.prisma.court.findFirst({
      where: {
        id: query.courtId,
        deletedAt: null,
        complex: {
          deletedAt: null
        }
      },
      select: {
        id: true,
        name: true,
        status: true,
        complex: {
          select: {
            status: true
          }
        },
        availability: {
          select: {
            startTime: true,
            endTime: true,
            days: {
              orderBy: {
                day: 'asc'
              },
              select: {
                day: true
              }
            }
          }
        },
        reservations: {
          where: {
            status: 'CONFIRMED',
            startsAt: {
              gte: dayStart,
              lt: dayEnd
            }
          },
          orderBy: {
            startsAt: 'asc'
          },
          select: {
            startsAt: true
          }
        }
      }
    });

    if (court == null) {
      throw new ReservationCourtNotFoundError(query.courtId);
    }

    return {
      court: {
        id: court.id,
        name: court.name,
        status: court.status,
        complexStatus: court.complex.status
      },
      availability:
        court.availability == null
          ? null
          : {
              days: court.availability.days.map(({ day }) => day),
              startTime: formatTimeOnly(court.availability.startTime),
              endTime: formatTimeOnly(court.availability.endTime)
            },
      confirmedStartsAt: (court.reservations as Array<{ startsAt: Date }>).map(({ startsAt }) =>
        startsAt.toISOString()
      )
    };
  }

  async createConfirmedReservation(
    command: ICreateConfirmedReservationCommand
  ): Promise<IReservationSnapshot> {
    try {
      const reservation = await this.prisma.reservation.create({
        data: {
          userId: command.userId,
          courtId: command.courtId,
          startsAt: command.startsAt,
          endsAt: command.endsAt,
          status: 'CONFIRMED'
        }
      });

      return {
        id: reservation.id,
        userId: reservation.userId,
        courtId: reservation.courtId,
        startsAt: reservation.startsAt.toISOString(),
        endsAt: reservation.endsAt.toISOString(),
        status: reservation.status
      };
    } catch (error) {
      if (isPrismaUniqueConstraintError(error)) {
        throw new ReservationConflictError(command.courtId, command.startsAt.toISOString());
      }

      throw error;
    }
  }

  async findMyReservationsByUserId(
    query: IFindMyReservationsQuery
  ): Promise<IMyReservationsSnapshotGroups> {
    const [upcomingReservations, finalizedReservations] = await Promise.all([
      this.prisma.reservation.findMany({
        where: {
          userId: query.userId,
          status: 'CONFIRMED',
          court: {
            deletedAt: null,
            complex: {
              deletedAt: null
            }
          }
        },
        orderBy: [{ startsAt: 'asc' }, { id: 'asc' }],
        take: query.upcomingLimit,
        select: {
          id: true,
          startsAt: true,
          endsAt: true,
          status: true,
          completedAt: true,
          review: {
            select: {
              id: true
            }
          },
          court: {
            select: {
              name: true,
              imageUpload: {
                select: {
                  objectKey: true
                }
              },
              complex: {
                select: {
                  name: true
                }
              }
            }
          }
        }
      }),
      this.prisma.reservation.findMany({
        where: {
          userId: query.userId,
          status: 'COMPLETED',
          completedAt: {
            not: null
          },
          court: {
            deletedAt: null,
            complex: {
              deletedAt: null
            }
          }
        },
        orderBy: [{ completedAt: 'desc' }, { startsAt: 'desc' }, { id: 'asc' }],
        take: query.finalizedLimit,
        select: {
          id: true,
          startsAt: true,
          endsAt: true,
          status: true,
          completedAt: true,
          review: {
            select: {
              id: true
            }
          },
          court: {
            select: {
              name: true,
              imageUpload: {
                select: {
                  objectKey: true
                }
              },
              complex: {
                select: {
                  name: true
                }
              }
            }
          }
        }
      })
    ]);

    return {
      upcoming: mapMyReservationSnapshots(upcomingReservations),
      finalized: mapMyReservationSnapshots(finalizedReservations)
    };
  }

  async listOwnerReservations(
    query: IListOwnerReservationsQuery
  ): Promise<IOwnerReservationsSnapshotGroups> {
    const ownedCourtIds = await this.loadOwnedCourtIds(query);

    if (query.court != null && !ownedCourtIds.includes(query.court.courtId)) {
      throw new OwnerReservationCourtNotAccessibleError(query.court.courtId);
    }

    if (ownedCourtIds.length === 0) {
      return { upcoming: [], finalized: [] };
    }

    // Narrow the read scope to the selected court when the filter is provided;
    // otherwise the scope remains the full set of owned court IDs.
    const scopedCourtIds =
      query.court != null ? [query.court.courtId] : ownedCourtIds;

    const [upcomingReservations, finalizedReservations] = await Promise.all([
      this.prisma.reservation.findMany({
        where: {
          courtId: { in: scopedCourtIds },
          status: 'CONFIRMED',
          court: {
            deletedAt: null,
            complex: {
              deletedAt: null
            }
          }
        },
        orderBy: [{ startsAt: 'asc' }, { id: 'asc' }],
        take: query.upcomingLimit,
        select: OWNER_RESERVATION_CARD_SELECT
      }),
      this.prisma.reservation.findMany({
        where: {
          courtId: { in: scopedCourtIds },
          status: 'COMPLETED',
          completedAt: {
            not: null
          },
          court: {
            deletedAt: null,
            complex: {
              deletedAt: null
            }
          }
        },
        orderBy: [{ completedAt: 'desc' }, { startsAt: 'desc' }, { id: 'asc' }],
        take: query.finalizedLimit,
        select: OWNER_RESERVATION_CARD_SELECT
      })
    ]);

    return {
      upcoming: mapOwnerReservationSnapshots(upcomingReservations),
      finalized: mapOwnerReservationSnapshots(finalizedReservations)
    };
  }

  private async loadOwnedCourtIds(
    query: IListOwnerReservationsQuery
  ): Promise<string[]> {
    const provider =
      query.ownerIdentity.provider ?? OWNER_RESERVATIONS_COGNITO_NATIVE_PROVIDER;
    const providerSubject = query.ownerIdentity.sub;

    const courts: { id: string }[] = await this.prisma.court.findMany({
      where: {
        deletedAt: null,
        complex: {
          deletedAt: null,
          owner: {
            identities: {
              some: {
                provider,
                providerSubject
              }
            }
          }
        }
      },
      select: { id: true }
    });

    return courts.map((court) => court.id);
  }

  async completeExpiredReservations(
    command: ICompleteExpiredReservationsCommand
  ): Promise<ICompletedReservationSnapshot[]> {
    const rows = await this.prisma.$queryRaw<Array<{ id: string; userId: string }>>(
      Prisma.sql`
        UPDATE "mejengueros_dev"."Reservation"
        SET
          "status" = CAST(${PRISMA_COMPLETED_RESERVATION_STATUS} AS "mejengueros_dev"."ReservationStatus"),
          "completedAt" = "endsAt",
          "updatedAt" = CURRENT_TIMESTAMP
        WHERE
          "status" = CAST(${PRISMA_CONFIRMED_RESERVATION_STATUS} AS "mejengueros_dev"."ReservationStatus")
          AND "endsAt" <= ${command.now}
        RETURNING "id", "userId"
      `
    );

    return rows.map((row) => ({
      id: row.id,
      userId: row.userId
    }));
  }
}

const PRISMA_CONFIRMED_RESERVATION_STATUS = 'CONFIRMED';
const PRISMA_COMPLETED_RESERVATION_STATUS = 'COMPLETED';

const OWNER_RESERVATION_CARD_SELECT = {
  id: true,
  startsAt: true,
  endsAt: true,
  status: true,
  completedAt: true,
  court: {
    select: {
      name: true,
      imageUpload: {
        select: {
          objectKey: true
        }
      },
      complex: {
        select: {
          name: true
        }
      }
    }
  }
} as const;

function mapOwnerReservationSnapshots(
  reservations: Array<{
    id: string;
    startsAt: Date;
    endsAt: Date;
    status: IOwnerReservationSnapshot['status'];
    completedAt: Date | null;
    court: {
      name: string;
      imageUpload: { objectKey: string } | null;
      complex: {
        name: string;
      };
    };
  }>
): IOwnerReservationSnapshot[] {
  return reservations.map((reservation) => ({
    id: reservation.id,
    complexName: reservation.court.complex.name,
    courtName: reservation.court.name,
    imageObjectKey: reservation.court.imageUpload?.objectKey ?? undefined,
    startsAt: reservation.startsAt.toISOString(),
    endsAt: reservation.endsAt.toISOString(),
    status: reservation.status
  }));
}

function mapMyReservationSnapshots(
  reservations: Array<{
    id: string;
    startsAt: Date;
    endsAt: Date;
    status: IMyReservationSnapshot['status'];
    completedAt: Date | null;
    review: { id: string } | null;
    court: {
      name: string;
      imageUpload: { objectKey: string } | null;
      complex: {
        name: string;
      };
    };
  }>
): IMyReservationSnapshot[] {
  return reservations.map((reservation) => ({
    id: reservation.id,
    complexName: reservation.court.complex.name,
    courtName: reservation.court.name,
    imageObjectKey: reservation.court.imageUpload?.objectKey ?? undefined,
    startsAt: reservation.startsAt.toISOString(),
    endsAt: reservation.endsAt.toISOString(),
    status: reservation.status,
    completedAt: reservation.completedAt?.toISOString() ?? null,
    reviewId: reservation.review?.id ?? null
  }));
}

function formatTimeOnly(value: Date): string {
  const hours = String(value.getUTCHours()).padStart(2, '0');
  const minutes = String(value.getUTCMinutes()).padStart(2, '0');

  return `${hours}:${minutes}`;
}

function isPrismaUniqueConstraintError(error: unknown): error is { code: 'P2002' } {
  return (
    typeof error === 'object' &&
    error !== null &&
    'code' in error &&
    error.code === 'P2002'
  );
}
