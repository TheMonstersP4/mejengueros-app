import { Inject, Injectable } from '@nestjs/common';
import { Prisma } from '../../../../generated/prisma/client';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import { InvalidReviewEvidenceUploadError } from '../../domain/errors/invalid-review-evidence-upload.error';
import { OwnerReviewCourtNotAccessibleError } from '../../domain/errors/owner-review-court-not-accessible.error';
import { ReviewAlreadyExistsError } from '../../domain/errors/review-already-exists.error';
import {
  type ICreatedReviewSnapshot,
  type ICreateReviewCommand,
  type IListOwnerCourtReviewsQuery,
  type IListOwnerCourtReviewsResult,
  type IListPublicCourtReviewsQuery,
  type IListPublicCourtReviewsResult,
  type IOwnerCourtReviewItem,
  type IOwnerCourtReviewsSummary,
  type IPublicCourtReviewItem,
  type IReservationForReviewSnapshot,
  type IReviewRepository,
  type IReviewableReservationSnapshot
} from '../../domain/repositories/review.repository';
import {
  buildReviewerDisplayName,
  buildReviewerInitials
} from '../../application/services/format-reviewer-identity';

const COGNITO_NATIVE_PROVIDER = 'Cognito';

interface IOwnerReviewRawClient {
  $queryRaw<T = unknown>(
    query: TemplateStringsArray | Prisma.Sql,
    ...values: unknown[]
  ): Promise<T>;
}

interface IOwnerReviewPersistenceClient extends IOwnerReviewRawClient {
  court: {
    findMany: PrismaService['court']['findMany'];
    findFirst: PrismaService['court']['findFirst'];
  };
  review: {
    findMany: PrismaService['review']['findMany'];
    count: PrismaService['review']['count'];
  };
}

type PublicReviewRow = {
  id: string;
  rating: number;
  comment: string | null;
  createdAt: Date;
  reservation: {
    user: {
      name: string | null;
    };
  };
};

interface IOwnerReviewsAggregateRow {
  average: number | string | null;
}

type OwnerReviewRow = {
  id: string;
  rating: number;
  comment: string | null;
  createdAt: Date;
  reservation: {
    court: {
      id: string;
      name: string;
    };
    user: {
      name: string | null;
    };
  };
};

interface IReviewPersistenceClient {
  reservation: PrismaService['reservation'];
  review: PrismaService['review'];
}

/**
 * Prisma-backed repository covering both owner-only review reads and the
 * player review submission flow (create + reservation lookups).
 */
@Injectable()
export class PrismaReviewRepository implements IReviewRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: IReviewPersistenceClient & IOwnerReviewPersistenceClient
  ) {}

  async listOwnerCourtReviews(
    query: IListOwnerCourtReviewsQuery
  ): Promise<IListOwnerCourtReviewsResult> {
    const ownedCourtIds = await this.loadOwnedCourtIds(query);

    if (query.court != null && !ownedCourtIds.includes(query.court.courtId)) {
      throw new OwnerReviewCourtNotAccessibleError(query.court.courtId);
    }

    if (ownedCourtIds.length === 0) {
      return this.emptyResult(query);
    }

    // Narrow the read scope to the selected court when the filter is provided;
    // otherwise the scope remains the full set of owned court IDs.
    const scopedCourtIds =
      query.court != null ? [query.court.courtId] : ownedCourtIds;

    const { page, pageSize } = query.pagination;
    const skip = (page - 1) * pageSize;
    const take = pageSize;

    const [totalItems, rows, aggregate] = await Promise.all([
      this.prisma.review.count({
        where: {
          reservation: {
            courtId: { in: scopedCourtIds }
          }
        }
      }),
      this.prisma.review.findMany({
        where: {
          reservation: {
            courtId: { in: scopedCourtIds }
          }
        },
        orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
        skip,
        take,
        select: {
          id: true,
          rating: true,
          comment: true,
          createdAt: true,
          reservation: {
            select: {
              court: { select: { id: true, name: true } },
              user: { select: { name: true } }
            }
          }
        }
      }),
      this.loadAggregateRating(scopedCourtIds)
    ]);

    const items: IOwnerCourtReviewItem[] = (rows as OwnerReviewRow[]).map((row) => ({
      reviewId: row.id,
      rating: row.rating,
      comment: row.comment,
      createdAt: row.createdAt.toISOString(),
      court: {
        id: row.reservation.court.id,
        name: row.reservation.court.name
      },
      reviewer: {
        displayName: buildReviewerDisplayName(row.reservation.user.name),
        initials: buildReviewerInitials(row.reservation.user.name)
      }
    }));

    const summary: IOwnerCourtReviewsSummary = {
      selectedCourtId: query.court?.courtId ?? null,
      totalReviews: totalItems,
      averageRating: this.normalizeAverage(aggregate?.average)
    };

    return {
      summary,
      items,
      totalItems,
      page,
      pageSize
    };
  }

  async findPubliclyVisibleCourtId(courtId: string): Promise<string | null> {
    const court = await this.prisma.court.findFirst({
      where: {
        id: courtId,
        status: 'ACTIVE',
        deletedAt: null,
        isPublished: true,
        complex: {
          status: 'ACTIVE',
          deletedAt: null,
          isPublished: true
        }
      },
      select: { id: true }
    });

    return court?.id ?? null;
  }

  async listPublicCourtReviews(
    query: IListPublicCourtReviewsQuery
  ): Promise<IListPublicCourtReviewsResult> {
    const { page, pageSize } = query.pagination;
    const skip = (page - 1) * pageSize;
    const take = pageSize;

    const [totalItems, rows, aggregate] = await Promise.all([
      this.prisma.review.count({
        where: {
          reservation: {
            courtId: query.courtId
          }
        }
      }),
      this.prisma.review.findMany({
        where: {
          reservation: {
            courtId: query.courtId
          }
        },
        orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
        skip,
        take,
        select: {
          id: true,
          rating: true,
          comment: true,
          createdAt: true,
          reservation: {
            select: {
              user: { select: { name: true } }
            }
          }
        }
      }),
      this.loadAggregateRating([query.courtId])
    ]);

    const items: IPublicCourtReviewItem[] = (rows as PublicReviewRow[]).map((row) => ({
      reviewId: row.id,
      rating: row.rating,
      comment: row.comment,
      createdAt: row.createdAt.toISOString(),
      reviewer: {
        displayName: buildReviewerDisplayName(row.reservation.user.name),
        initials: buildReviewerInitials(row.reservation.user.name)
      }
    }));

    return {
      summary: {
        totalReviews: totalItems,
        averageRating: this.normalizeAverage(aggregate?.average)
      },
      items,
      totalItems,
      page,
      pageSize
    };
  }

  async findLatestReviewableReservationForUser(
    userId: string
  ): Promise<IReviewableReservationSnapshot | null> {
    const reservation = await this.prisma.reservation.findFirst({
      where: {
        userId,
        status: 'COMPLETED',
        completedAt: { not: null },
        review: null,
        court: {
          deletedAt: null,
          complex: { deletedAt: null }
        }
      },
      orderBy: [{ completedAt: 'desc' }, { startsAt: 'desc' }],
      select: {
        id: true,
        startsAt: true,
        endsAt: true,
        court: {
          select: {
            name: true,
            imageUpload: {
              select: { objectKey: true }
            },
            complex: {
              select: { name: true }
            }
          }
        }
      }
    });

    if (reservation == null) {
      return null;
    }

    return {
      reservationId: reservation.id,
      complexName: reservation.court.complex.name,
      courtName: reservation.court.name,
      startsAt: reservation.startsAt.toISOString(),
      endsAt: reservation.endsAt.toISOString(),
      imageObjectKey: reservation.court.imageUpload?.objectKey ?? undefined
    };
  }

  async findReservationById(
    reservationId: string
  ): Promise<IReservationForReviewSnapshot | null> {
    const reservation = await this.prisma.reservation.findUnique({
      where: { id: reservationId },
      select: {
        id: true,
        userId: true,
        status: true,
        completedAt: true,
        review: {
          select: { id: true }
        }
      }
    });

    if (reservation == null) {
      return null;
    }

    return {
      id: reservation.id,
      userId: reservation.userId,
      status: reservation.status,
      completedAt: reservation.completedAt?.toISOString() ?? null,
      reviewId: reservation.review?.id ?? null
    };
  }

  async findReviewIdByEvidenceImageUploadId(
    evidenceImageUploadId: string
  ): Promise<string | null> {
    const review = await this.prisma.review.findFirst({
      where: { evidenceImageUploadId },
      select: { id: true }
    });

    return review?.id ?? null;
  }

  async createReview(command: ICreateReviewCommand): Promise<ICreatedReviewSnapshot> {
    try {
      const review = await this.prisma.review.create({
        data: {
          reservationId: command.reservationId,
          rating: command.rating,
          comment: command.comment,
          evidenceImageUploadId: command.evidenceImageUploadId
        }
      });

      return {
        id: review.id,
        reservationId: review.reservationId,
        rating: review.rating,
        comment: review.comment ?? undefined,
        evidenceImageUploadId: review.evidenceImageUploadId ?? undefined,
        createdAt: review.createdAt.toISOString()
      };
    } catch (error) {
      if (this.isUniqueConstraintOn(error, 'reservationId')) {
        throw new ReviewAlreadyExistsError(command.reservationId);
      }

      if (this.isUniqueConstraintOn(error, 'evidenceImageUploadId')) {
        throw InvalidReviewEvidenceUploadError.alreadyAssigned(
          command.evidenceImageUploadId ?? ''
        );
      }

      throw error;
    }
  }

  private emptyResult(query: IListOwnerCourtReviewsQuery): IListOwnerCourtReviewsResult {
    return {
      summary: {
        selectedCourtId: query.court?.courtId ?? null,
        totalReviews: 0,
        averageRating: null
      },
      items: [],
      totalItems: 0,
      page: query.pagination.page,
      pageSize: query.pagination.pageSize
    };
  }

  private async loadOwnedCourtIds(
    query: IListOwnerCourtReviewsQuery
  ): Promise<string[]> {
    const provider = query.ownerIdentity.provider ?? COGNITO_NATIVE_PROVIDER;
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

  private async loadAggregateRating(
    ownedCourtIds: string[]
  ): Promise<IOwnerReviewsAggregateRow | null> {
    const rows = await this.prisma.$queryRaw<IOwnerReviewsAggregateRow[]>(Prisma.sql`
      SELECT
        AVG(review.rating)::float8 AS average
      FROM mejengueros_dev."Review" review
      INNER JOIN mejengueros_dev."Reservation" reservation
        ON reservation.id = review."reservationId"
      WHERE reservation."courtId" IN (${Prisma.join(ownedCourtIds)})
    `);

    return rows[0] ?? null;
  }

  private normalizeAverage(value: number | string | null | undefined): number | null {
    if (value == null) {
      return null;
    }

    const numeric = typeof value === 'string' ? Number(value) : value;

    if (!Number.isFinite(numeric)) {
      return null;
    }

    return Number(numeric.toFixed(1));
  }

  private isUniqueConstraintOn(error: unknown, field: string): boolean {
    if (
      typeof error !== 'object' ||
      error == null ||
      !('code' in error) ||
      error.code !== 'P2002' ||
      !('meta' in error)
    ) {
      return false;
    }

    const meta = error.meta as { target?: string | string[] } | undefined;
    const targets = Array.isArray(meta?.target)
      ? meta.target
      : meta?.target == null
        ? []
        : [meta.target];

    return targets.some((target) => String(target).includes(field));
  }
}
