import { Inject, Injectable, Optional } from '@nestjs/common';
import {
  FILE_READ_URL_PORT,
  type IFileReadUrlPort
} from '@/modules/files/application/ports/file-read-url.port';
import { buildReservableSlots } from '@/modules/reservations/domain/services/reservation-slot-policy';
import {
  costaRicaBusinessDayBounds,
  formatCostaRicaBusinessDate
} from '@/shared/domain/time/costa-rica-business-time';
import { Prisma } from '../../../../generated/prisma/client';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import { InvalidCourtCatalogLocationFilterError } from '../../domain/errors/invalid-court-catalog-location-filter.error';
import type {
  ICourtCatalogFilters,
  ICourtCatalogItem,
  ICourtCatalogPage,
  ICourtCatalogRepository
} from '../../domain/repositories/court-catalog.repository';

export const COURT_CATALOG_TODAY_PROVIDER = Symbol('COURT_CATALOG_TODAY_PROVIDER');

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
      latitude: true,
      longitude: true,
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
      startTime: true,
      endTime: true,
      days: {
        select: {
          day: true
        }
      }
    }
  },
  reservations: {
    select: {
      startsAt: true
    }
  },
  imageUpload: {
    select: {
      objectKey: true
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
    findFirst(args: Prisma.CantonFindFirstArgs): Promise<{ id: string } | null>;
  };
  court: {
    findMany(args: Prisma.CourtFindManyArgs): Promise<PublicCourtCatalogRow[]>;
    count(args: Prisma.CourtCountArgs): Promise<number>;
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
    @Inject(FILE_READ_URL_PORT)
    private readonly fileReadUrl: IFileReadUrlPort,
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

  async listPublicCatalog(filters: ICourtCatalogFilters): Promise<ICourtCatalogPage> {
    const normalizedQuery = filters.q?.trim();
    const now = this.todayProvider();
    const todayBusinessDate = formatCostaRicaBusinessDate(now);
    const todayBusinessDayBounds = costaRicaBusinessDayBounds(todayBusinessDate);
    const { page, pageSize } = filters.pagination;
    const skip = (page - 1) * pageSize;

    // Shared filter used by both the count and the page read so the total and
    // the returned items always describe the same result set.
    const where: Prisma.CourtWhereInput = {
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
        : {}),
      // A court "offers" a service when it is attached to the court itself or to
      // its complex. Each selected service becomes its own AND clause so the
      // court must offer ALL of them, and AND composes with the text-search OR
      // above instead of overwriting it (two OR keys on one object would clash).
      ...(filters.serviceIds && filters.serviceIds.length > 0
        ? {
            AND: filters.serviceIds.map((serviceId) => ({
              OR: [
                { services: { some: { serviceCatalogId: serviceId } } },
                {
                  complex: {
                    services: { some: { serviceCatalogId: serviceId } }
                  }
                }
              ]
            }))
          }
        : {})
    };

    const [totalItems, courts] = await Promise.all([
      this.prisma.court.count({ where }),
      this.prisma.court.findMany({
        where,
        skip,
        take: pageSize,
        // The trailing `id` keeps the ordering deterministic so courts are
        // never skipped or duplicated across incremental page reads.
        orderBy: [{ complex: { name: 'asc' } }, { name: 'asc' }, { id: 'asc' }],
        select: {
          ...PUBLIC_COURT_CATALOG_SELECT,
          reservations: {
            where: {
              status: 'CONFIRMED',
              startsAt: {
                gte: todayBusinessDayBounds.start,
                lt: todayBusinessDayBounds.end
              }
            },
            select: {
              startsAt: true
            }
          }
        }
      }) as Promise<PublicCourtCatalogRow[]>
    ]);

    const catalogCourts = courts.filter(
      (court) => court.complex.province !== null && court.complex.canton !== null
    );
    const ratingsByCourtId = await this.loadRatingsByCourtId(
      catalogCourts.map((court) => court.id)
    );

    const items = await Promise.all(
      catalogCourts.map(async (court) => {
        const rating = ratingsByCourtId.get(court.id) ?? { average: null, count: 0 };
        const services = uniqueSortedServices([
          ...court.services.map(
            (service: PublicCourtCatalogRow['services'][number]) => service.serviceCatalog.name
          ),
          ...court.complex.services.map(
            (service: PublicCourtCatalogRow['complex']['services'][number]) =>
              service.serviceCatalog.name
          )
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
          latitude: court.complex.latitude ?? null,
          longitude: court.complex.longitude ?? null,
          services,
          rating,
          isReservableToday: hasReservableSlotToday(court, todayBusinessDate, now),
          imageUrl: await this.createImageUrl(court.imageUpload?.objectKey ?? null)
        } satisfies ICourtCatalogItem;
      })
    );

    return {
      items,
      totalItems,
      page,
      pageSize
    };
  }

  private async createImageUrl(objectKey: string | null): Promise<string | null> {
    if (!objectKey) {
      return null;
    }

    return this.fileReadUrl.createReadUrl(objectKey);
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

function hasReservableSlotToday(
  court: PublicCourtCatalogRow,
  todayDate: string,
  now: Date
): boolean {
  if (court.availability == null) {
    return false;
  }

  return (
    buildReservableSlots(
      {
        court: {
          id: court.id,
          name: court.name,
          status: 'ACTIVE',
          complexStatus: 'ACTIVE'
        },
        availability: {
          days: court.availability.days.map(({ day }) => day),
          startTime: formatTimeOnly(court.availability.startTime),
          endTime: formatTimeOnly(court.availability.endTime)
        },
        confirmedStartsAt: court.reservations.map(({ startsAt }) => startsAt.toISOString())
      },
      todayDate,
      now
    ).availabilityStatus === 'AVAILABLE'
  );
}

function formatTimeOnly(value: Date): string {
  const hours = String(value.getUTCHours()).padStart(2, '0');
  const minutes = String(value.getUTCMinutes()).padStart(2, '0');

  return `${hours}:${minutes}`;
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
