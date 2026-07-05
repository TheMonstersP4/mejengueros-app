import { Inject, Injectable } from '@nestjs/common';
import { Prisma } from '../../../../generated/prisma/client';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import { OwnerReviewCourtNotAccessibleError } from '../../domain/errors/owner-review-court-not-accessible.error';
import {
  type IListOwnerCourtReviewsQuery,
  type IListOwnerCourtReviewsResult,
  type IOwnerCourtReviewItem,
  type IOwnerCourtReviewsSummary,
  type IReviewRepository
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
  };
  review: {
    findMany: PrismaService['review']['findMany'];
    count: PrismaService['review']['count'];
  };
}

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

/**
 * Prisma-backed repository for owner-only review reads.
 */
@Injectable()
export class PrismaReviewRepository implements IReviewRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: IOwnerReviewPersistenceClient
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
}
