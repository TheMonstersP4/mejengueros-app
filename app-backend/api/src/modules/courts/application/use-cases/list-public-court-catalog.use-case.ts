import { Inject, Injectable } from '@nestjs/common';
import {
  type ICourtCatalogFilters,
  type ICourtCatalogItem,
  type ICourtCatalogRepository,
  COURT_CATALOG_REPOSITORY
} from '../../domain/repositories/court-catalog.repository';

@Injectable()
export class ListPublicCourtCatalogUseCase {
  constructor(
    @Inject(COURT_CATALOG_REPOSITORY)
    private readonly courtCatalogRepository: ICourtCatalogRepository
  ) {}

  async execute(filters: ICourtCatalogFilters): Promise<ICourtCatalogItem[]> {
    if (filters.provinceId && filters.cantonId) {
      await this.courtCatalogRepository.assertProvinceAndCantonMatch(
        filters.provinceId,
        filters.cantonId
      );
    }

    return this.courtCatalogRepository.listPublicCatalog(filters);
  }
}
