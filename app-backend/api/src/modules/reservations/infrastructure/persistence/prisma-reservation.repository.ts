import { Inject, Injectable } from '@nestjs/common';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import { ReservationConflictError } from '../../domain/errors/reservation-conflict.error';
import { ReservationCourtNotFoundError } from '../../domain/errors/reservation-court-not-found.error';
import type {
  ICreateConfirmedReservationCommand,
  IReservationRepository,
  IReservationSnapshot,
  IReservationWindowQuery,
  IReservationWindowSnapshot
} from '../../domain/repositories/reservation.repository';

interface IReservationPersistenceClient {
  court: {
    findFirst: PrismaService['court']['findFirst'];
  };
  reservation: {
    create: PrismaService['reservation']['create'];
  };
}

@Injectable()
export class PrismaReservationRepository implements IReservationRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: IReservationPersistenceClient
  ) {}

  async getReservationWindow(
    query: IReservationWindowQuery
  ): Promise<IReservationWindowSnapshot> {
    const dayStart = new Date(`${query.date}T00:00:00.000Z`);
    const dayEnd = new Date(`${query.date}T00:00:00.000Z`);
    dayEnd.setUTCDate(dayEnd.getUTCDate() + 1);

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
      confirmedStartsAt: court.reservations.map(({ startsAt }) => startsAt.toISOString())
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
