import { Inject, Injectable } from '@nestjs/common';
import { Prisma } from '@/generated/prisma/client';
import type { NotificationStatus, NotificationType } from '@/generated/prisma/enums';
import { PrismaService } from '@/shared/infrastructure/database/prisma.service';
import type {
  INotificationRepository,
  INotificationSnapshot,
  IReviewPromptReservationCandidate
} from '../../domain/repositories/notification.repository';

interface INotificationPersistenceClient {
  notification: PrismaService['notification'];
  reservation: PrismaService['reservation'];
}

type ReviewPromptReservationRow = {
  id: string;
  userId: string;
  startsAt: Date;
  endsAt: Date;
  court: {
    name: string;
    complex: {
      name: string;
    };
  };
};

type NotificationWithReservationRow = {
  id: string;
  userId: string;
  reservationId: string;
  type: NotificationType;
  status: NotificationStatus;
  createdAt: Date;
  readAt: Date | null;
  reservation: {
    startsAt: Date;
    endsAt: Date;
    court: {
      name: string;
      complex: {
        name: string;
      };
    };
  };
};

/**
 * Prisma-backed notification repository.
 */
@Injectable()
export class PrismaNotificationRepository implements INotificationRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: INotificationPersistenceClient
  ) {}

  async createReviewPromptNotifications(
    reservations: IReviewPromptReservationCandidate[]
  ): Promise<INotificationSnapshot[]> {
    const reservationIds = reservations.map((reservation) => reservation.id);

    if (reservationIds.length === 0) {
      return [];
    }

    const eligibleReservations = await this.prisma.reservation.findMany({
      where: {
        id: { in: reservationIds },
        review: null
      },
      select: {
        id: true,
        userId: true,
        startsAt: true,
        endsAt: true,
        court: {
          select: {
            name: true,
            complex: {
              select: {
                name: true
              }
            }
          }
        }
      }
    });

    const existingNotifications = await this.prisma.notification.findMany({
      where: {
        reservationId: { in: reservationIds },
        type: REVIEW_PROMPT_NOTIFICATION_TYPE
      },
      select: {
        reservationId: true
      }
    });
    const existingReservationIds = new Set(
      existingNotifications.map((notification) => notification.reservationId)
    );
    const createdNotifications: INotificationSnapshot[] = [];

    for (const reservation of eligibleReservations as ReviewPromptReservationRow[]) {
      if (existingReservationIds.has(reservation.id)) {
        continue;
      }

      try {
        const notification = await this.prisma.notification.create({
          data: {
            userId: reservation.userId,
            reservationId: reservation.id,
            type: REVIEW_PROMPT_NOTIFICATION_TYPE
          }
        });

        createdNotifications.push(
          mapNotificationWithReservation(notification, reservation)
        );
      } catch (error) {
        if (!isUniqueConstraintError(error)) {
          throw error;
        }
      }
    }

    return createdNotifications;
  }

  async listForUser(userId: string): Promise<INotificationSnapshot[]> {
    const notifications = await this.prisma.notification.findMany({
      where: { userId },
      orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
      select: NOTIFICATION_WITH_RESERVATION_SELECT
    });

    return (notifications as NotificationWithReservationRow[]).map(
      mapNotificationWithReservationRow
    );
  }

  async markRead(
    notificationId: string,
    userId: string
  ): Promise<INotificationSnapshot | null> {
    const notification = await this.prisma.notification.findFirst({
      where: {
        id: notificationId,
        userId
      },
      select: {
        id: true
      }
    });

    if (notification == null) {
      return null;
    }

    const updatedNotification = await this.prisma.notification.update({
      where: { id: notificationId },
      data: {
        status: 'READ',
        readAt: new Date()
      },
      select: NOTIFICATION_WITH_RESERVATION_SELECT
    });

    return mapNotificationWithReservationRow(
      updatedNotification as NotificationWithReservationRow
    );
  }
}

const REVIEW_PROMPT_NOTIFICATION_TYPE = 'REVIEW_PROMPT';

const NOTIFICATION_WITH_RESERVATION_SELECT = {
  id: true,
  userId: true,
  reservationId: true,
  type: true,
  status: true,
  createdAt: true,
  readAt: true,
  reservation: {
    select: {
      startsAt: true,
      endsAt: true,
      court: {
        select: {
          name: true,
          complex: {
            select: {
              name: true
            }
          }
        }
      }
    }
  }
} as const;

function mapNotificationWithReservation(
  notification: {
    id: string;
    userId: string;
    reservationId: string;
    type: NotificationType;
    status: NotificationStatus;
    createdAt: Date;
    readAt: Date | null;
  },
  reservation: ReviewPromptReservationRow
): INotificationSnapshot {
  return {
    id: notification.id,
    userId: notification.userId,
    reservationId: notification.reservationId,
    type: notification.type,
    status: notification.status,
    complexName: reservation.court.complex.name,
    courtName: reservation.court.name,
    startsAt: reservation.startsAt.toISOString(),
    endsAt: reservation.endsAt.toISOString(),
    createdAt: notification.createdAt.toISOString(),
    readAt: notification.readAt?.toISOString() ?? null
  };
}

function mapNotificationWithReservationRow(
  notification: NotificationWithReservationRow
): INotificationSnapshot {
  return {
    id: notification.id,
    userId: notification.userId,
    reservationId: notification.reservationId,
    type: notification.type,
    status: notification.status,
    complexName: notification.reservation.court.complex.name,
    courtName: notification.reservation.court.name,
    startsAt: notification.reservation.startsAt.toISOString(),
    endsAt: notification.reservation.endsAt.toISOString(),
    createdAt: notification.createdAt.toISOString(),
    readAt: notification.readAt?.toISOString() ?? null
  };
}

function isUniqueConstraintError(error: unknown): boolean {
  return error instanceof Prisma.PrismaClientKnownRequestError && error.code === 'P2002';
}
