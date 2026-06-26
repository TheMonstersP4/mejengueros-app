import { Inject, Injectable } from '@nestjs/common';
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

interface ICourtCatalogPersistenceClient {
  canton: {
    findFirst: PrismaService['canton']['findFirst'];
  };
  court: {
    findMany: PrismaService['court']['findMany'];
  };
}

@Injectable()
export class PrismaCourtCatalogRepository implements ICourtCatalogRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: ICourtCatalogPersistenceClient
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
    const today = WEEKDAY_BY_INDEX[new Date().getDay()];
    const courts = await this.prisma.court.findMany({
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
      orderBy: [{ complex: { name: 'asc' } }, { name: 'asc' }],
      select: {
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
        },
        reservations: {
          select: {
            review: {
              select: {
                rating: true
              }
            }
          }
        }
      }
    });

    return courts
      .filter((court) => court.complex.province !== null && court.complex.canton !== null)
      .map((court) => {
        const ratings = court.reservations
          .map((reservation) => reservation.review?.rating)
          .filter((rating): rating is number => rating !== null && rating !== undefined);
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
          rating: {
            average:
              ratings.length > 0
                ? Number((ratings.reduce((sum, rating) => sum + rating, 0) / ratings.length).toFixed(1))
                : null,
            count: ratings.length
          },
          isReservableToday:
            court.availability?.days.some((availabilityDay) => availabilityDay.day === today) ??
            false,
          imageUrl: null
        } satisfies ICourtCatalogItem;
      });
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
