import { ListCantonsByProvinceUseCase } from '@/modules/locations/application/use-cases/list-cantons-by-province.use-case';
import { ListProvincesUseCase } from '@/modules/locations/application/use-cases/list-provinces.use-case';
import { ListPublicCourtCatalogUseCase } from '@/modules/courts/application/use-cases/list-public-court-catalog.use-case';
import { PrismaCourtCatalogRepository } from '@/modules/courts/infrastructure/persistence/prisma-court-catalog.repository';
import { CourtsController } from '@/modules/courts/interfaces/http/controllers/courts.controller';
import { PrismaLocationCatalogRepository } from '@/modules/locations/infrastructure/persistence/prisma-location-catalog.repository';
import { LocationsController } from '@/modules/locations/interfaces/http/controllers/locations.controller';
import { ListActiveServicesUseCase } from '@/modules/service-catalog/application/use-cases/list-active-services.use-case';
import { PrismaServiceCatalogRepository } from '@/modules/service-catalog/infrastructure/persistence/prisma-service-catalog.repository';
import { ServiceCatalogController } from '@/modules/service-catalog/interfaces/http/controllers/service-catalog.controller';

describe('catalog modules behavior', () => {
  it('delegates locations controllers and use cases to the repository', async () => {
    const repository = {
      listProvinces: jest.fn().mockResolvedValue([{ id: 'province-id', code: 'SJ', name: 'San José' }]),
      listCantonsByProvince: jest
        .fn()
        .mockResolvedValue([
          {
            id: 'canton-id',
            provinceId: 'province-id',
            code: 'SJ-ESC',
            name: 'Escazú'
          }
        ])
    };
    const listProvinces = new ListProvincesUseCase(repository);
    const listCantonsByProvince = new ListCantonsByProvinceUseCase(repository);
    const controller = new LocationsController(listProvinces, listCantonsByProvince);

    await expect(controller.listProvincesCatalog()).resolves.toEqual([
      { id: 'province-id', code: 'SJ', name: 'San José' }
    ]);
    await expect(controller.listCantonsCatalog('province-id')).resolves.toEqual([
      {
        id: 'canton-id',
        provinceId: 'province-id',
        code: 'SJ-ESC',
        name: 'Escazú'
      }
    ]);
  });

  it('builds sorted Prisma queries for provinces and cantons', async () => {
    const prisma = {
      province: {
        findMany: jest.fn().mockResolvedValue([])
      },
      canton: {
        findMany: jest.fn().mockResolvedValue([])
      }
    };
    const repository = new PrismaLocationCatalogRepository(prisma as never);

    await repository.listProvinces();
    await repository.listCantonsByProvince('province-id');

    expect(prisma.province.findMany).toHaveBeenCalledWith({
      orderBy: { name: 'asc' },
      select: { id: true, code: true, name: true }
    });
    expect(prisma.canton.findMany).toHaveBeenCalledWith({
      where: { provinceId: 'province-id' },
      orderBy: { name: 'asc' },
      select: { id: true, provinceId: true, code: true, name: true }
    });
  });

  it('delegates service-catalog controller and filters active services by scope', async () => {
    const repository = {
      listActiveServices: jest
        .fn()
        .mockResolvedValue([{ id: 'service-id', name: 'Lighting', scope: 'COURT' }])
    };
    const useCase = new ListActiveServicesUseCase(repository);
    const controller = new ServiceCatalogController(useCase);

    await expect(controller.list({ scope: 'COURT' })).resolves.toEqual([
      { id: 'service-id', name: 'Lighting', scope: 'COURT' }
    ]);
    expect(repository.listActiveServices).toHaveBeenCalledWith('COURT');
  });

  it('builds Prisma queries for active service catalogs', async () => {
    const prisma = {
      serviceCatalog: {
        findMany: jest.fn().mockResolvedValue([])
      }
    };
    const repository = new PrismaServiceCatalogRepository(prisma as never);

    await repository.listActiveServices('COMPLEX');

    expect(prisma.serviceCatalog.findMany).toHaveBeenCalledWith({
      where: {
        isActive: true,
        scope: 'COMPLEX'
      },
      orderBy: [{ scope: 'asc' }, { name: 'asc' }],
      select: { id: true, name: true, scope: true }
    });
  });

  it('validates province/canton filters and delegates public court catalog listing', async () => {
    const repository = {
      assertProvinceAndCantonMatch: jest.fn().mockResolvedValue(undefined),
      listPublicCatalog: jest.fn().mockResolvedValue([
        {
          courtId: 'court-id',
          courtName: 'Court A',
          complexId: 'complex-id',
          complexName: 'North Sports Center',
          province: { id: 'province-id', name: 'San José' },
          canton: { id: 'canton-id', name: 'Escazú' },
          services: ['Sintetico'],
          rating: { average: 4.5, count: 2 },
          isReservableToday: true,
          imageUrl: null
        }
      ])
    };
    const useCase = new ListPublicCourtCatalogUseCase(repository);
    const controller = new CourtsController(useCase);

    await expect(
      controller.listCatalog({ q: 'north', provinceId: 'province-id', cantonId: 'canton-id' })
    ).resolves.toEqual([
      expect.objectContaining({
        courtId: 'court-id',
        services: ['Sintetico']
      })
    ]);
    expect(repository.assertProvinceAndCantonMatch).toHaveBeenCalledWith(
      'province-id',
      'canton-id'
    );
    expect(repository.listPublicCatalog).toHaveBeenCalledWith({
      q: 'north',
      provinceId: 'province-id',
      cantonId: 'canton-id'
    });
  });

  it('builds Prisma queries for the public court catalog and validates province/canton pairs', async () => {
    const prisma = {
      $queryRaw: jest.fn().mockResolvedValue([{ courtId: 'court-id', average: 4.5, count: 2 }]),
      canton: {
        findFirst: jest.fn().mockResolvedValue({ id: 'canton-id' })
      },
      court: {
        findMany: jest.fn().mockResolvedValue([
          {
            id: 'court-id',
            name: 'Court A',
            services: [{ serviceCatalog: { name: 'Sintetico' } }],
            complex: {
              id: 'complex-id',
              name: 'North Sports Center',
              province: { id: 'province-id', name: 'San José' },
              canton: { id: 'canton-id', name: 'Escazú' },
              services: [{ serviceCatalog: { name: 'Parqueo' } }]
            },
            availability: {
              days: [{ day: ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'][new Date().getDay()] }]
            }
          }
        ])
      }
    };
    const repository = new PrismaCourtCatalogRepository(prisma as never);

    await repository.assertProvinceAndCantonMatch('province-id', 'canton-id');
    await expect(
      repository.listPublicCatalog({
        q: 'north',
        provinceId: 'province-id',
        cantonId: 'canton-id'
      })
    ).resolves.toEqual([
      expect.objectContaining({
        courtId: 'court-id',
        rating: { average: 4.5, count: 2 }
      })
    ]);

    expect(prisma.canton.findFirst).toHaveBeenCalledWith({
      where: { id: 'canton-id', provinceId: 'province-id' },
      select: { id: true }
    });
    expect(prisma.court.findMany).toHaveBeenCalledWith(
      expect.objectContaining({
        where: expect.objectContaining({
          status: 'ACTIVE',
          deletedAt: null,
          isPublished: true,
          complex: expect.objectContaining({
            status: 'ACTIVE',
            deletedAt: null,
            isPublished: true,
            provinceId: 'province-id',
            cantonId: 'canton-id'
          })
        }),
        take: 50,
        orderBy: [{ complex: { name: 'asc' } }, { name: 'asc' }],
        select: expect.objectContaining({
          complex: expect.any(Object),
          services: expect.any(Object),
          availability: expect.any(Object)
        })
      })
    );
    expect(prisma.court.findMany.mock.calls[0][0].select).not.toHaveProperty('reservations');
    expect(prisma.$queryRaw).toHaveBeenCalledTimes(1);
    expect(prisma.$queryRaw.mock.calls[0]?.[0]?.strings?.join(' ')).toContain(
      'AVG(review.rating)::float8 AS average'
    );
  });

  it('returns empty rating aggregates when a court has no reviews', async () => {
    const prisma = {
      $queryRaw: jest.fn().mockResolvedValue([]),
      canton: {
        findFirst: jest.fn().mockResolvedValue({ id: 'canton-id' })
      },
      court: {
        findMany: jest.fn().mockResolvedValue([
          {
            id: 'court-id',
            name: 'Court A',
            services: [],
            complex: {
              id: 'complex-id',
              name: 'North Sports Center',
              province: { id: 'province-id', name: 'San José' },
              canton: { id: 'canton-id', name: 'Escazú' },
              services: []
            },
            availability: null
          }
        ])
      }
    };
    const repository = new PrismaCourtCatalogRepository(prisma as never);

    await expect(repository.listPublicCatalog({})).resolves.toEqual([
      expect.objectContaining({
        courtId: 'court-id',
        rating: { average: null, count: 0 }
      })
    ]);
  });
});
