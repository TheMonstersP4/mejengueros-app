import { Inject, Injectable } from '@nestjs/common';
import type { ICantonCatalogItem, ILocationCatalogRepository } from '../../domain/repositories/location-catalog.repository';
import { LOCATION_CATALOG_REPOSITORY } from '../../domain/repositories/location-catalog.repository';

/**
 * Returns controlled cantons for a selected province.
 */
@Injectable()
export class ListCantonsByProvinceUseCase {
  constructor(
    @Inject(LOCATION_CATALOG_REPOSITORY)
    private readonly locationCatalogRepository: ILocationCatalogRepository
  ) {}

  execute(provinceId: string): Promise<ICantonCatalogItem[]> {
    return this.locationCatalogRepository.listCantonsByProvince(provinceId);
  }
}
