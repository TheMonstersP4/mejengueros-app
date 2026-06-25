import { Inject, Injectable } from '@nestjs/common';
import type { ILocationCatalogRepository, IProvinceCatalogItem } from '../../domain/repositories/location-catalog.repository';
import { LOCATION_CATALOG_REPOSITORY } from '../../domain/repositories/location-catalog.repository';

/**
 * Returns controlled provinces available for the complex wizard.
 */
@Injectable()
export class ListProvincesUseCase {
  constructor(
    @Inject(LOCATION_CATALOG_REPOSITORY)
    private readonly locationCatalogRepository: ILocationCatalogRepository
  ) {}

  execute(): Promise<IProvinceCatalogItem[]> {
    return this.locationCatalogRepository.listProvinces();
  }
}
