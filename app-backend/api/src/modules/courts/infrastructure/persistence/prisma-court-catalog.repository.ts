import { Inject, Injectable, Optional } from '@nestjs/common';
import { Prisma } from '../../../../generated/prisma/client';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import { InvalidCourtCatalogLocationFilterError } from '../../domain/errors/invalid-court-catalog-location-filter.error';
import type {
  ICourtCatalogFilters,
  ICourtCatalogItem,
  ICourtCatalogRepository
} from '../../domain/repositories/court-catalog.repository';

const WEEKDAY_BY_INDEX = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY'
] as const;

const PUBLIC_COURT_CATALOG_TAKE = 50;

export const COURT_CATALOG_TODAY_PROVIDER = Symbol('COURT_CATALOG_TODAY_PROVIDER');

function weekdayInUtc(date: Date): (typeof WEEKDAY_BY_INDEX)[number] {
  return WEEKDAY_BY_INDEX[date.getUTCDay()];
}

const PUBLIC_COURT_CATALOG_SELECT = {
  id: true,
  name: true,
  services: {
    select: {
      serviceCatalog: {
        select: {
          name: true
        }
      }
    }
  },
  complex: {
    select: {
      id: true,
      name: true,
      province: {
        select: {
          id: true,
          name: true
        }
      },
      canton: {
        select: {
          id: true,
          name: true
        }
      },
      services: {
        select: {
          serviceCatalog: {
            select: {
              name: true
            }
          }
        }
      }
    }
  },
  availability: {
    select: {
      days: {
        select: {
          day: true
        }
      }
    }
  }
} satisfies Prisma.CourtSelect;

type PublicCourtCatalogRow = Prisma.CourtGetPayload<{
  select: typeof PUBLIC_COURT_CATALOG_SELECT;
}>;

interface ICourtCatalogPersistenceClient {
  $queryRaw<T = unknown>(
    query: TemplateStringsArray | Prisma.Sql,
    ...values: unknown[]
  ): Promise<T>;
  canton: {
    findFirst: PrismaService['canton']['findFirst'];
  };
  court: {
    findMany: PrismaService['court']['findMany'];
  };
}

interface ICourtRatingAggregateRow {
  courtId: string;
  average: number;
  count: number;
}

@Injectable()
export class PrismaCourtCatalogRepository implements ICourtCatalogRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: ICourtCatalogPersistenceClient,
    @Optional()
    @Inject(COURT_CATALOG_TODAY_PROVIDER)
    private readonly todayProvider: () => Date = () => new Date()
  ) {}

  async assertProvinceAndCantonMatch(
    provinceId: string,
    cantonId: string
  ): Promise<void> {
    const canton = await this.prisma.canton.findFirst({
      where: {
        id: cantonId,
        provinceId
      },
      select: { id: true }
    });

    if (!canton) {
      throw InvalidCourtCatalogLocationFilterError.cantonOutsideProvince(
        provinceId,
        cantonId
      );
    }
  }

  async listPublicCatalog(filters: ICourtCatalogFilters): Promise<ICourtCatalogItem[]> {
    const normalizedQuery = filters.q?.trim();
    const today = weekdayInUtc(this.todayProvider());
    const courts = (await this.prisma.court.findMany({
      where: {
        status: 'ACTIVE',
        deletedAt: null,
        isPublished: true,
        complex: {
          status: 'ACTIVE',
          deletedAt: null,
          isPublished: true,
          provinceId: filters.provinceId,
          cantonId: filters.cantonId,
          province: {
            isNot: null
          },
          canton: {
            isNot: null
          },
          ...(normalizedQuery
            ? {
                OR: [
                  {
                    name: {
                      contains: normalizedQuery,
                      mode: 'insensitive'
                    }
                  },
                  {
                    courts: {
                      some: {
                        name: {
                          contains: normalizedQuery,
                          mode: 'insensitive'
                        },
                        status: 'ACTIVE',
                        deletedAt: null,
                        isPublished: true
                      }
                    }
                  }
                ]
              }
            : {})
        },
        ...(normalizedQuery
          ? {
              OR: [
                {
                  name: {
                    contains: normalizedQuery,
                    mode: 'insensitive'
                  }
                },
                {
                  complex: {
                    name: {
                      contains: normalizedQuery,
                      mode: 'insensitive'
                    }
                  }
                }
              ]
            }
          : {})
      },
      take: PUBLIC_COURT_CATALOG_TAKE,
      orderBy: [{ complex: { name: 'asc' } }, { name: 'asc' }],
      select: PUBLIC_COURT_CATALOG_SELECT
    })) as PublicCourtCatalogRow[];

    const catalogCourts = courts.filter(
      (court) => court.complex.province !== null && court.complex.canton !== null
    );
    const ratingsByCourtId = await this.loadRatingsByCourtId(
      catalogCourts.map((court) => court.id)
    );

    return catalogCourts.map((court) => {
      const rating = ratingsByCourtId.get(court.id) ?? { average: null, count: 0 };
      const services = uniqueSortedServices([
        ...court.services.map((service) => service.serviceCatalog.name),
        ...court.complex.services.map((service) => service.serviceCatalog.name)
      ]);

      return {
        courtId: court.id,
        courtName: court.name,
        complexId: court.complex.id,
        complexName: court.complex.name,
        province: {
          id: court.complex.province!.id,
          name: court.complex.province!.name
        },
        canton: {
          id: court.complex.canton!.id,
          name: court.complex.canton!.name
        },
        services,
        rating,
        isReservableToday:
          court.availability?.days.some((availabilityDay) => availabilityDay.day === today) ??
          false,
        imageUrl: null
      } satisfies ICourtCatalogItem;
    });
  }

  private async loadRatingsByCourtId(
    courtIds: string[]
  ): Promise<Map<string, ICourtCatalogItem['rating']>> {
    if (courtIds.length === 0) {
      return new Map();
    }

    const rows = await this.prisma.$queryRaw<ICourtRatingAggregateRow[]>(Prisma.sql`
      SELECT
        reservation."courtId" AS "courtId",
        AVG(review.rating)::float8 AS average,
        COUNT(*)::int AS count
      FROM mejengueros_dev."Review" review
      INNER JOIN mejengueros_dev."Reservation" reservation
        ON reservation.id = review."reservationId"
      WHERE reservation."courtId" IN (${Prisma.join(courtIds)})
      GROUP BY reservation."courtId"
    `);

    return new Map(
      rows.map((row) => [
        row.courtId,
        {
          average: Number(row.average.toFixed(1)),
          count: Number(row.count)
        }
      ])
    );
  }
}

function uniqueSortedServices(services: string[]): string[] {
  return Array.from(new Set(services)).sort((left, right) => {
    const leftPriority = surfacePriority(left);
    const rightPriority = surfacePriority(right);

    if (leftPriority !== rightPriority) {
      return leftPriority - rightPriority;
    }

    return left.localeCompare(right);
  });
}

function surfacePriority(serviceName: string): number {
  const normalized = serviceName.toLowerCase();

  if (normalized.includes('sint')) {
    return 0;
  }

  if (normalized.includes('hibr')) {
    return 1;
  }

  if (normalized.includes('natur')) {
    return 2;
  }

  return 3;
}
