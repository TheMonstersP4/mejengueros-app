import { Inject, Injectable } from '@nestjs/common';
import { PrismaService } from '../../../../shared/infrastructure/database/prisma.service';
import type {
  ICantonCatalogItem,
  ILocationCatalogRepository,
  IProvinceCatalogItem
} from '../../domain/repositories/location-catalog.repository';

interface ILocationCatalogPersistenceClient {
  province: {
    findMany: PrismaService['province']['findMany'];
  };
  canton: {
    findMany: PrismaService['canton']['findMany'];
  };
}

/**
 * Prisma-backed read model for controlled wizard locations.
 */
@Injectable()
export class PrismaLocationCatalogRepository implements ILocationCatalogRepository {
  constructor(
    @Inject(PrismaService)
    private readonly prisma: ILocationCatalogPersistenceClient
  ) {}

  async listProvinces(): Promise<IProvinceCatalogItem[]> {
    return this.prisma.province.findMany({
      orderBy: {
        name: 'asc'
      },
      select: {
        id: true,
        code: true,
        name: true
      }
    });
  }

  async listCantonsByProvince(provinceId: string): Promise<ICantonCatalogItem[]> {
    return this.prisma.canton.findMany({
      where: {
        provinceId
      },
      orderBy: {
        name: 'asc'
      },
      select: {
        id: true,
        provinceId: true,
        code: true,
        name: true
      }
    });
  }
}
