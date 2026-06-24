import { ListCantonsByProvinceUseCase } from '@/modules/locations/application/use-cases/list-cantons-by-province.use-case';
import { ListProvincesUseCase } from '@/modules/locations/application/use-cases/list-provinces.use-case';
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
});
