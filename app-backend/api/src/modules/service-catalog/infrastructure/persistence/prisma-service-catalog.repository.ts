import { Inject, Injectable } from '@nestjs/common';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import type {
  IServiceCatalogItem,
  IServiceCatalogRepository,
  IServiceCatalogScope
} from '../../domain/repositories/service-catalog.repository';

interface IServiceCatalogPersistenceClient {
  serviceCatalog: {
    findMany: PrismaService['serviceCatalog']['findMany'];
  };
}

/**
 * Prisma-backed read model for active wizard service catalogs.
 */
@Injectable()
export class PrismaServiceCatalogRepository implements IServiceCatalogRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: IServiceCatalogPersistenceClient
  ) {}

  async listActiveServices(
    scope?: IServiceCatalogScope
  ): Promise<IServiceCatalogItem[]> {
    return this.prisma.serviceCatalog.findMany({
      where: {
        isActive: true,
        ...(scope ? { scope } : {})
      },
      orderBy: [
        {
          scope: 'asc'
        },
        {
          name: 'asc'
        }
      ],
      select: {
        id: true,
        name: true,
        scope: true
      }
    });
  }
}
