import { Inject, Injectable } from '@nestjs/common';
import {
  type ICourtCatalogFilters,
  type ICourtCatalogPage,
  type ICourtCatalogRepository,
  COURT_CATALOG_REPOSITORY
} from '../../domain/repositories/court-catalog.repository';

/**
 * Default `pageSize` for the public court catalog endpoint when omitted.
 *
 * Kept small so the mobile infinite scroll loads the catalog in perceptible
 * increments instead of a single large batch.
 */
export const PUBLIC_COURT_CATALOG_DEFAULT_PAGE_SIZE = 10;

/**
 * Maximum allowed `pageSize` for the public court catalog endpoint.
 */
export const PUBLIC_COURT_CATALOG_MAX_PAGE_SIZE = 50;

/**
 * Maximum allowed `page` number for the public court catalog endpoint.
 *
 * Caps the `OFFSET` produced by `(page - 1) * pageSize` to prevent huge
 * offset abuse against the underlying court table.
 */
export const PUBLIC_COURT_CATALOG_MAX_PAGE = 10_000;

/**
 * Request shape accepted by the public court catalog use case.
 */
export interface IListPublicCourtCatalogRequest {
  q?: string;
  provinceId?: string;
  cantonId?: string;
  serviceIds?: string[];
  minRating?: number;
  page: number;
  pageSize: number;
}

@Injectable()
export class ListPublicCourtCatalogUseCase {
  constructor(
    @Inject(COURT_CATALOG_REPOSITORY)
    private readonly courtCatalogRepository: ICourtCatalogRepository
  ) {}

  async execute(
    request: IListPublicCourtCatalogRequest
  ): Promise<ICourtCatalogPage> {
    if (request.provinceId && request.cantonId) {
      await this.courtCatalogRepository.assertProvinceAndCantonMatch(
        request.provinceId,
        request.cantonId
      );
    }

    const filters: ICourtCatalogFilters = {
      q: request.q,
      provinceId: request.provinceId,
      cantonId: request.cantonId,
      serviceIds: request.serviceIds,
      minRating: request.minRating,
      pagination: {
        page: request.page,
        pageSize: request.pageSize
      }
    };

    return this.courtCatalogRepository.listPublicCatalog(filters);
  }
}
