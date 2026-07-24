import { ListCantonsByProvinceUseCase } from '@/modules/locations/application/use-cases/list-cantons-by-province.use-case';
import { ListProvincesUseCase } from '@/modules/locations/application/use-cases/list-provinces.use-case';
import { ListPublicCourtCatalogUseCase } from '@/modules/courts/application/use-cases/list-public-court-catalog.use-case';
import { PrismaCourtCatalogRepository } from '@/modules/courts/infrastructure/persistence/prisma-court-catalog.repository';
import type { IFileReadUrlPort } from '@/modules/files/application/ports/file-read-url.port';
import { CourtsController } from '@/modules/courts/interfaces/http/controllers/courts.controller';
import { StorageInspectionError } from '@/modules/files/infrastructure/errors/storage-inspection.error';
import { PrismaLocationCatalogRepository } from '@/modules/locations/infrastructure/persistence/prisma-location-catalog.repository';
import { LocationsController } from '@/modules/locations/interfaces/http/controllers/locations.controller';
import { ListActiveServicesUseCase } from '@/modules/service-catalog/application/use-cases/list-active-services.use-case';
import { PrismaServiceCatalogRepository } from '@/modules/service-catalog/infrastructure/persistence/prisma-service-catalog.repository';
import { ServiceCatalogController } from '@/modules/service-catalog/interfaces/http/controllers/service-catalog.controller';


const DEFAULT_PAGINATION = { page: 1, pageSize: 20 };
const FIXED_MONDAY = new Date('2026-06-22T12:00:00.000Z');
const CATALOG_IMAGE_READ_URL = 'https://read.example.test/courts/court-id.jpg';
const COSTA_RICA_UTC_ROLLOVER_INSTANT = '2026-07-05T02:24:00.000Z';
const COSTA_RICA_DAY_WINDOW_START_UTC = '2026-07-04T06:00:00.000Z';
const NEXT_COSTA_RICA_DAY_WINDOW_START_UTC = '2026-07-05T06:00:00.000Z';

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

  it('validates province/canton filters and delegates paginated public court catalog listing', async () => {
    const repository = {
      assertProvinceAndCantonMatch: jest.fn().mockResolvedValue(undefined),
      listPublicCatalog: jest.fn().mockResolvedValue({
        items: [
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
        ],
        totalItems: 42,
        page: 2,
        pageSize: 20
      })
    };
    const useCase = new ListPublicCourtCatalogUseCase(repository);
    const controller = new CourtsController(useCase);

    const response = await controller.listCatalog({
      q: 'north',
      provinceId: 'province-id',
      cantonId: 'canton-id',
      page: 2,
      pageSize: 20
    });

    expect(response.data).toEqual([
      expect.objectContaining({
        courtId: 'court-id',
        services: ['Sintetico']
      })
    ]);
    expect(response.meta?.pagination).toEqual({
      page: 2,
      pageSize: 20,
      totalItems: 42,
      totalPages: 3
    });
    expect(repository.assertProvinceAndCantonMatch).toHaveBeenCalledWith(
      'province-id',
      'canton-id'
    );
    expect(repository.listPublicCatalog).toHaveBeenCalledWith({
      q: 'north',
      provinceId: 'province-id',
      cantonId: 'canton-id',
      pagination: { page: 2, pageSize: 20 }
    });
  });

  it('forwards the service filters to the catalog repository', async () => {
    const repository = {
      assertProvinceAndCantonMatch: jest.fn().mockResolvedValue(undefined),
      listPublicCatalog: jest.fn().mockResolvedValue({
        items: [],
        totalItems: 0,
        page: 1,
        pageSize: 20
      })
    };
    const useCase = new ListPublicCourtCatalogUseCase(repository);
    const controller = new CourtsController(useCase);

    await controller.listCatalog({
      serviceIds: ['service-a', 'service-b'],
      page: 1,
      pageSize: 20
    });

    expect(repository.listPublicCatalog).toHaveBeenCalledWith({
      q: undefined,
      provinceId: undefined,
      cantonId: undefined,
      serviceIds: ['service-a', 'service-b'],
      minRating: undefined,
      pagination: { page: 1, pageSize: 20 }
    });
    // No province/canton pair, so only the service filter is applied.
    expect(repository.assertProvinceAndCantonMatch).not.toHaveBeenCalled();
  });

  it('forwards the minimum rating filter to the catalog repository', async () => {
    const repository = {
      assertProvinceAndCantonMatch: jest.fn().mockResolvedValue(undefined),
      listPublicCatalog: jest.fn().mockResolvedValue({
        items: [],
        totalItems: 0,
        page: 1,
        pageSize: 20
      })
    };
    const useCase = new ListPublicCourtCatalogUseCase(repository);
    const controller = new CourtsController(useCase);

    await controller.listCatalog({
      minRating: 4,
      page: 1,
      pageSize: 20
    });

    expect(repository.listPublicCatalog).toHaveBeenCalledWith({
      q: undefined,
      provinceId: undefined,
      cantonId: undefined,
      serviceIds: undefined,
      minRating: 4,
      pagination: { page: 1, pageSize: 20 }
    });
  });

  it('exposes empty pagination metadata when the catalog has no matches', async () => {
    const repository = {
      assertProvinceAndCantonMatch: jest.fn().mockResolvedValue(undefined),
      listPublicCatalog: jest.fn().mockResolvedValue({
        items: [],
        totalItems: 0,
        page: 1,
        pageSize: 20
      })
    };
    const useCase = new ListPublicCourtCatalogUseCase(repository);
    const controller = new CourtsController(useCase);

    const response = await controller.listCatalog({ page: 1, pageSize: 20 });

    expect(response.data).toEqual([]);
    expect(response.meta?.pagination).toEqual({
      page: 1,
      pageSize: 20,
      totalItems: 0,
      totalPages: 0
    });
    // No location pair is provided, so the province/canton guard is skipped.
    expect(repository.assertProvinceAndCantonMatch).not.toHaveBeenCalled();
  });

  it('builds Prisma queries for the public court catalog and validates province/canton pairs', async () => {
    const fileStorage = createFileReadUrlMock();
    const prisma = {
      $queryRaw: jest.fn().mockResolvedValue([{ courtId: 'court-id', average: 4.5, count: 2 }]),
      canton: {
        findFirst: jest.fn().mockResolvedValue({ id: 'canton-id' })
      },
      court: {
        count: jest.fn().mockResolvedValue(1),
        findMany: jest.fn().mockResolvedValue([
          {
            id: 'court-id',
            name: 'Court A',
            services: [{ serviceCatalog: { name: 'Sintetico' } }],
            complex: {
              id: 'complex-id',
              name: 'North Sports Center',
              latitude: 9.935,
              longitude: -84.091,
              province: { id: 'province-id', name: 'San José' },
              canton: { id: 'canton-id', name: 'Escazú' },
              services: [{ serviceCatalog: { name: 'Parqueo' } }]
            },
            availability: {
              startTime: new Date('1970-01-01T18:00:00.000Z'),
              endTime: new Date('1970-01-01T21:00:00.000Z'),
              days: [{ day: 'MONDAY' }]
            },
            reservations: [],
            imageUpload: {
              objectKey: 'test/uploads/court-image/court-id.jpg'
            }
          }
        ])
      }
    };
    const repository = new PrismaCourtCatalogRepository(
      prisma as never,
      fileStorage,
      () => FIXED_MONDAY
    );

    await repository.assertProvinceAndCantonMatch('province-id', 'canton-id');
    await expect(
      repository.listPublicCatalog({
        q: 'north',
        provinceId: 'province-id',
        cantonId: 'canton-id',
        pagination: { page: 2, pageSize: 20 }
      })
    ).resolves.toEqual({
      items: [
        expect.objectContaining({
          courtId: 'court-id',
          latitude: 9.935,
          longitude: -84.091,
          rating: { average: 4.5, count: 2 },
          isReservableToday: true
        })
      ],
      totalItems: 1,
      page: 2,
      pageSize: 20
    });

    expect(prisma.canton.findFirst).toHaveBeenCalledWith({
      where: { id: 'canton-id', provinceId: 'province-id' },
      select: { id: true }
    });
    expect(prisma.court.count).toHaveBeenCalledWith(
      expect.objectContaining({
        where: expect.objectContaining({
          status: 'ACTIVE',
          deletedAt: null,
          isPublished: true
        })
      })
    );
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
        skip: 20,
        take: 20,
        orderBy: [{ complex: { name: 'asc' } }, { name: 'asc' }, { id: 'asc' }],
        select: expect.objectContaining({
          complex: expect.any(Object),
          services: expect.any(Object),
          availability: expect.any(Object),
          imageUpload: {
            select: {
              objectKey: true
            }
          }
        })
      })
    );
    expect(prisma.$queryRaw).toHaveBeenCalledTimes(1);
    expect(fileStorage.createReadUrl).toHaveBeenCalledWith(
      'test/uploads/court-image/court-id.jpg'
    );
    expect(prisma.$queryRaw.mock.calls[0]?.[0]?.strings?.join(' ')).toContain(
      'AVG(review.rating)::float8 AS average'
    );
  });

  it('requires courts to offer every selected service on the court or its complex', async () => {
    const fileStorage = createFileReadUrlMock();
    const prisma = {
      $queryRaw: jest.fn().mockResolvedValue([]),
      canton: {
        findFirst: jest.fn().mockResolvedValue(null)
      },
      court: {
        count: jest.fn().mockResolvedValue(0),
        findMany: jest.fn().mockResolvedValue([])
      }
    };
    const repository = new PrismaCourtCatalogRepository(
      prisma as never,
      fileStorage,
      () => FIXED_MONDAY
    );

    await repository.listPublicCatalog({
      serviceIds: ['service-a', 'service-b'],
      pagination: DEFAULT_PAGINATION
    });

    // One AND clause per service => the court must offer ALL of them.
    const expectedServiceFilter = {
      AND: [
        {
          OR: [
            { services: { some: { serviceCatalogId: 'service-a' } } },
            {
              complex: {
                services: { some: { serviceCatalogId: 'service-a' } }
              }
            }
          ]
        },
        {
          OR: [
            { services: { some: { serviceCatalogId: 'service-b' } } },
            {
              complex: {
                services: { some: { serviceCatalogId: 'service-b' } }
              }
            }
          ]
        }
      ]
    };
    expect(prisma.court.count).toHaveBeenCalledWith(
      expect.objectContaining({
        where: expect.objectContaining(expectedServiceFilter)
      })
    );
    expect(prisma.court.findMany).toHaveBeenCalledWith(
      expect.objectContaining({
        where: expect.objectContaining(expectedServiceFilter)
      })
    );
    // A service-only filter never triggers the province/canton pairing guard.
    expect(prisma.canton.findFirst).not.toHaveBeenCalled();
  });

  it('restricts the catalog to courts meeting the minimum rating', async () => {
    const fileStorage = createFileReadUrlMock();
    const prisma = {
      // First raw call resolves the qualifying court ids (HAVING AVG >= min).
      $queryRaw: jest
        .fn()
        .mockResolvedValue([{ courtId: 'court-a' }, { courtId: 'court-b' }]),
      canton: {
        findFirst: jest.fn().mockResolvedValue(null)
      },
      court: {
        count: jest.fn().mockResolvedValue(0),
        findMany: jest.fn().mockResolvedValue([])
      }
    };
    const repository = new PrismaCourtCatalogRepository(
      prisma as never,
      fileStorage,
      () => FIXED_MONDAY
    );

    await repository.listPublicCatalog({
      minRating: 4,
      pagination: DEFAULT_PAGINATION
    });

    // The rating threshold is resolved through an aggregate query, rounded to one
    // decimal to match the card, with the minimum passed as a bound parameter.
    const ratingCall = prisma.$queryRaw.mock.calls[0]?.[0];
    expect(ratingCall?.strings?.join(' ')).toContain('HAVING');
    expect(ratingCall?.strings?.join(' ')).toContain('ROUND(AVG(review.rating)');
    expect(ratingCall?.values).toEqual([4]);
    // Count and page reads are constrained to the qualifying ids so pagination
    // and totals stay consistent.
    const expectedIdFilter = { id: { in: ['court-a', 'court-b'] } };
    expect(prisma.court.count).toHaveBeenCalledWith(
      expect.objectContaining({ where: expect.objectContaining(expectedIdFilter) })
    );
    expect(prisma.court.findMany).toHaveBeenCalledWith(
      expect.objectContaining({ where: expect.objectContaining(expectedIdFilter) })
    );
  });

  it('returns empty rating aggregates when a court has no reviews', async () => {
    const fileStorage = createFileReadUrlMock();
    const prisma = {
      $queryRaw: jest.fn().mockResolvedValue([]),
      canton: {
        findFirst: jest.fn().mockResolvedValue({ id: 'canton-id' })
      },
      court: {
        count: jest.fn().mockResolvedValue(1),
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
            availability: null,
            reservations: [],
            imageUpload: null
          }
        ])
      }
    };
    const repository = new PrismaCourtCatalogRepository(prisma as never, fileStorage);

    await expect(
      repository.listPublicCatalog({ pagination: DEFAULT_PAGINATION })
    ).resolves.toMatchObject({
      items: [
        expect.objectContaining({
        courtId: 'court-id',
        rating: { average: null, count: 0 },
        isReservableToday: false,
        imageUrl: null
        })
      ]
    });
    expect(fileStorage.createReadUrl).not.toHaveBeenCalled();
  });

  it('keeps each catalog court imageUrl matched to its own object key when read URLs resolve out of order', async () => {
    const fileStorage = createFileReadUrlMock();
    fileStorage.createReadUrl.mockImplementation((objectKey: string) =>
      objectKey === 'test/uploads/court-image/court-a.jpg'
        ? new Promise((resolve) => setTimeout(() => resolve('https://read.example.test/courts/court-a.jpg'), 10))
        : Promise.resolve('https://read.example.test/courts/court-b.jpg')
    );
    const prisma = {
      $queryRaw: jest.fn().mockResolvedValue([]),
      canton: {
        findFirst: jest.fn()
      },
      court: {
        count: jest.fn().mockResolvedValue(1),
        findMany: jest.fn().mockResolvedValue([
          createCatalogCourtRow({
            id: 'court-a',
            name: 'Court A',
            imageObjectKey: 'test/uploads/court-image/court-a.jpg'
          }),
          createCatalogCourtRow({
            id: 'court-b',
            name: 'Court B',
            imageObjectKey: 'test/uploads/court-image/court-b.jpg'
          })
        ])
      }
    };
    const repository = new PrismaCourtCatalogRepository(prisma as never, fileStorage);

    await expect(
      repository.listPublicCatalog({ pagination: DEFAULT_PAGINATION })
    ).resolves.toMatchObject({
      items: [
        expect.objectContaining({
        courtId: 'court-a',
        imageUrl: 'https://read.example.test/courts/court-a.jpg'
      }),
      expect.objectContaining({
        courtId: 'court-b',
        imageUrl: 'https://read.example.test/courts/court-b.jpg'
      })
      ]
    });
    expect(fileStorage.createReadUrl.mock.calls).toEqual([
      ['test/uploads/court-image/court-a.jpg'],
      ['test/uploads/court-image/court-b.jpg']
    ]);
  });

  it('propagates catalog image signer failures instead of degrading imageUrl to null', async () => {
    const fileStorage = createFileReadUrlMock();
    fileStorage.createReadUrl.mockRejectedValue(
      new StorageInspectionError('test/uploads/court-image/court-id.jpg', new Error('signer failed'))
    );
    const prisma = {
      $queryRaw: jest.fn().mockResolvedValue([]),
      canton: {
        findFirst: jest.fn()
      },
      court: {
        count: jest.fn().mockResolvedValue(1),
        findMany: jest.fn().mockResolvedValue([
          createCatalogCourtRow({
            id: 'court-id',
            name: 'Court A',
            imageObjectKey: 'test/uploads/court-image/court-id.jpg'
          })
        ])
      }
    };
    const repository = new PrismaCourtCatalogRepository(prisma as never, fileStorage);

    await expect(repository.listPublicCatalog({ pagination: DEFAULT_PAGINATION })).rejects.toThrow(StorageInspectionError);
  });

  it('calculates isReservableToday from the injected date provider', async () => {
    const fileStorage = createFileReadUrlMock();
    const prisma = {
      $queryRaw: jest.fn().mockResolvedValue([]),
      canton: {
        findFirst: jest.fn()
      },
      court: {
        count: jest.fn().mockResolvedValue(1),
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
            availability: {
              startTime: new Date('1970-01-01T18:00:00.000Z'),
              endTime: new Date('1970-01-01T21:00:00.000Z'),
              days: [{ day: 'TUESDAY' }]
            },
            reservations: [],
            imageUpload: null
          }
        ])
      }
    };
    const repository = new PrismaCourtCatalogRepository(
      prisma as never,
      fileStorage,
      () => FIXED_MONDAY
    );

    await expect(
      repository.listPublicCatalog({ pagination: DEFAULT_PAGINATION })
    ).resolves.toMatchObject({
      items: [
        expect.objectContaining({
        courtId: 'court-id',
        isReservableToday: false
        })
      ]
    });
  });

  it('uses the Costa Rica business weekday for isReservableToday near UTC rollover boundaries', async () => {
    const fileStorage = createFileReadUrlMock();
    const prisma = {
      $queryRaw: jest.fn().mockResolvedValue([]),
      canton: {
        findFirst: jest.fn()
      },
      court: {
        count: jest.fn().mockResolvedValue(1),
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
            availability: {
              startTime: new Date('1970-01-01T18:00:00.000Z'),
              endTime: new Date('1970-01-01T21:00:00.000Z'),
              days: [{ day: 'MONDAY' }]
            },
            reservations: [],
            imageUpload: null
          }
        ])
      }
    };
    const repository = new PrismaCourtCatalogRepository(
      prisma as never,
      fileStorage,
      () => new Date('2026-06-23T00:30:00.000Z')
    );

    await expect(
      repository.listPublicCatalog({ pagination: DEFAULT_PAGINATION })
    ).resolves.toMatchObject({
      items: [
        expect.objectContaining({
        courtId: 'court-id',
        isReservableToday: true
        })
      ]
    });
  });

  it('uses real same-day slot availability when calculating isReservableToday', async () => {
    const fileStorage = createFileReadUrlMock();
    const prisma = {
      $queryRaw: jest.fn().mockResolvedValue([]),
      canton: {
        findFirst: jest.fn()
      },
      court: {
        count: jest.fn().mockResolvedValue(1),
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
            availability: {
              startTime: new Date('1970-01-01T18:00:00.000Z'),
              endTime: new Date('1970-01-01T21:00:00.000Z'),
              days: [{ day: 'MONDAY' }]
            },
            reservations: [{ startsAt: new Date('2026-06-23T01:00:00.000Z') }],
            imageUpload: null
          }
        ])
      }
    };
    const repository = new PrismaCourtCatalogRepository(
      prisma as never,
      fileStorage,
      () => new Date('2026-06-22T23:45:00.000Z')
    );

    await expect(
      repository.listPublicCatalog({ pagination: DEFAULT_PAGINATION })
    ).resolves.toMatchObject({
      items: [
        expect.objectContaining({
        courtId: 'court-id',
        isReservableToday: true
        })
      ]
    });
  });

  it('locks catalog reservation queries to Costa Rica day bounds near UTC rollover', async () => {
    const fileStorage = createFileReadUrlMock();
    const findMany = jest.fn().mockResolvedValue([
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
        availability: {
          startTime: new Date('1970-01-01T21:00:00.000Z'),
          endTime: new Date('1970-01-01T23:00:00.000Z'),
          days: [{ day: 'SATURDAY' }]
        },
        reservations: [],
        imageUpload: null
      }
    ]);
    const prisma = {
      $queryRaw: jest.fn().mockResolvedValue([]),
      canton: {
        findFirst: jest.fn()
      },
      court: {
        count: jest.fn().mockResolvedValue(1),
        findMany
      }
    };
    const repository = new PrismaCourtCatalogRepository(
      prisma as never,
      fileStorage,
      () => new Date(COSTA_RICA_UTC_ROLLOVER_INSTANT)
    );

    await expect(
      repository.listPublicCatalog({ pagination: DEFAULT_PAGINATION })
    ).resolves.toMatchObject({
      items: [
        expect.objectContaining({
        courtId: 'court-id',
        isReservableToday: true
        })
      ]
    });

    const startsAtBounds = findMany.mock.calls[0]?.[0]?.select?.reservations?.where?.startsAt;

    expect(startsAtBounds?.gte.toISOString()).toBe(COSTA_RICA_DAY_WINDOW_START_UTC);
    expect(startsAtBounds?.lt.toISOString()).toBe(NEXT_COSTA_RICA_DAY_WINDOW_START_UTC);
  });

  function createFileReadUrlMock(): jest.Mocked<IFileReadUrlPort> {
    return {
      createReadUrl: jest.fn().mockResolvedValue(CATALOG_IMAGE_READ_URL)
    };
  }

  function createCatalogCourtRow({
    id,
    name,
    imageObjectKey
  }: {
    id: string;
    name: string;
    imageObjectKey: string | null;
  }) {
    return {
      id,
      name,
      services: [],
      complex: {
        id: 'complex-id',
        name: 'North Sports Center',
        province: { id: 'province-id', name: 'San José' },
        canton: { id: 'canton-id', name: 'Escazú' },
        services: []
      },
      availability: null,
      reservations: [],
      imageUpload: imageObjectKey ? { objectKey: imageObjectKey } : null
    };
  }
});
